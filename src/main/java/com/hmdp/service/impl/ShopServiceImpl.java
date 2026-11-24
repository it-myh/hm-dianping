package com.hmdp.service.impl;

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

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

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
        // 定义缓存key
        String key = CACHE_SHOP_KEY + id;
        // 1.查看缓存是否有该商铺信息
        String jsonShop = stringRedisTemplate.opsForValue().get(key);
        // 2.如果缓存有，直接返回
        if(StrUtil.isNotBlank(jsonShop)){
            return Result.ok(JSONUtil.toBean(jsonShop, Shop.class));
        }
        // 走到这里说明jsonShop，要么为null，“”，“  ”，这三种情况，排除为空串的情况
        if(jsonShop != null){
            return Result.fail("请停止访问没有的商铺！,不然将你封号！");
        }
        // 3.如果缓存没有，从数据库查询
        Shop shop = getById(id);
        if(shop == null){
            // 如果是id下面没有商铺也将其加入缓存中，防止大量的请求访问数据库，导致缓存穿透
            stringRedisTemplate.opsForValue().set(key, "");
            return Result.fail("商铺不存在");
        }
        // 4.如果数据库有，写入缓存，下次就可以直接返回了
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
        // 5.然后返回商铺信息
        return Result.ok(shop);
    }
}
