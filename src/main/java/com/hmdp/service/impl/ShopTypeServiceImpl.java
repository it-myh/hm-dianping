package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryAllShopType() {
        // 1.查看缓存是否有该商铺信息
        String key = CACHE_SHOP_TYPE_KEY;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)){
            // 2.如果有，直接返回
            return Result.ok(JSONUtil.toList(json, ShopType.class));
        }
        // 3.如果没有，从数据库中查询所有商铺类型
        List<ShopType> shopTypeList = query().select().orderByAsc("sort").list();
        // 4.数据库中没有数据，返回错误
        if(shopTypeList == null){
            return Result.fail("数据库中没有数据");
        }
        // 5.如果数据库中有数据，就加入缓存中
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypeList));
        // 6.返回结果
        return Result.ok(shopTypeList);
    }
}
