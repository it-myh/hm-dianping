package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryById(Long id) {
        // 这个解决缓存的穿透问题
        //Shop shop = getShopWithNullCache(id);
        // 这个是加上互斥锁，防止多个线程同时查询数据库，进行缓存的重建
        Shop shop = getShopWithMutexLock(id);
        if(shop == null){
            return Result.fail("商铺不存在!");
        }
        return Result.ok(shop);
    }
    @Override
    public Result updateByShop(Shop shop) {
        // 1.先判断商铺id是否存在
        Long id = shop.getId();
        if(id == null){
            return Result.fail("商铺id不能为空");
        }
        // 2.先更新数据库
        updateById(shop);
        // 3.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
    /**
     * 解决缓存穿透问题，将id下面没有商铺也将其加入缓存中，防止大量的请求访问数据库，导致缓存穿透
     * @param id
     * @return
     */
    private Shop getShopWithNullCache(Long id) {
        // 定义缓存key
        String key = CACHE_SHOP_KEY + id;
        // 1.查看缓存是否有该商铺信息
        String jsonShop = stringRedisTemplate.opsForValue().get(key);
        // 2.如果缓存有，直接返回
        if(StrUtil.isNotBlank(jsonShop)){
            return JSONUtil.toBean(jsonShop, Shop.class);
        }
        // 走到这里说明jsonShop，要么为null，“”，“  ”，这三种情况，排除为空串的情况
        if(jsonShop != null){
            return null;
        }
        // 3.如果缓存没有，从数据库查询
        Shop shop = getById(id);
        if(shop == null){
            // 如果是id下面没有商铺也将其加入缓存中，防止大量的请求访问数据库，导致缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 4.如果数据库有，写入缓存，下次就可以直接返回了
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 5.然后返回商铺信息
        return shop;
    }

    /**
     * 使用互斥锁，解决缓存击穿问题
     * @param id
     * @return
     */
    private Shop getShopWithMutexLock(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // ====================== 1. 第一次无锁查缓存（快速响应，大部分请求在这里返回） ======================
        String jsonShop = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(jsonShop)){
            return JSONUtil.toBean(jsonShop, Shop.class);
        }
        if(jsonShop != null){ // 空值缓存（防穿透），直接返回null
            return null;
        }

        // ====================== 2. while自旋拿互斥锁（防击穿核心，不要使用递归，递归次数无限 → 直接 StackOverflowError！） ======================
        String lockKey = LOCK_SHOP_KEY + id;
        while (!BooleanUtil.isTrue(
                stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS)
        )) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // ====================== 3. 持锁期间的业务逻辑（try-finally保证锁释放） ======================
        try {
            // 🔑 双重检查：拿到锁后再查一次缓存（防止第一次查缓存和拿到锁之间的并发窗口漏洞）
            jsonShop = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(jsonShop)){
                return JSONUtil.toBean(jsonShop, Shop.class);
            }
            // 🔑 空值缓存二次检查：如果缓存是空串（说明之前有线程查过DB，商铺不存在），直接返回null
            if(jsonShop != null){
                return null;
            }
            // 缓存还是空 → 查DB + 重建缓存/空值缓存
            Shop shop = getById(id);
            if(shop == null){
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return shop;
        } finally {
            // 🔑 必须释放锁！不管业务逻辑成功/失败/异常，都要执行
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 使用逻辑过期时间，解决缓存击穿问题
     * @param id
     * @return
     */
    private Shop getShopWithLogicalExpire(Long id) {
        // 定义缓存key
        String key = CACHE_SHOP_KEY + id;
        // 1.查看缓存是否有该商铺信息
        String jsonShop = stringRedisTemplate.opsForValue().get(key);
        // 2.如果缓存有，直接返回
        if(StrUtil.isNotBlank(jsonShop)){
            return JSONUtil.toBean(jsonShop, Shop.class);
        }
        // 走到这里说明jsonShop，要么为null，“”，“  ”，这三种情况，排除为空串的情况
        if(jsonShop != null){
            return null;
        }
        // 3.如果缓存没有，从数据库查询
        Shop shop = getById(id);
        if(shop == null){
            // 如果是id下面没有商铺也将其加入缓存中，防止大量的请求访问数据库，导致缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 4.如果数据库有，写入缓存，下次就可以直接返回了
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 5.然后返回商铺信息
        return shop;
    }
}
