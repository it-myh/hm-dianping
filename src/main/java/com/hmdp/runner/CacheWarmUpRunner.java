package com.hmdp.runner;

import com.hmdp.service.impl.ShopServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

@Slf4j
@Component
public class CacheWarmUpRunner implements CommandLineRunner {

    @Resource
    private ShopServiceImpl shopServiceImpl;

    @Override
    public void run(String... args) throws Exception {
        log.info("===== 开始商铺缓存预热 =====");
        // 这里写你要预热的热点商铺id(1~14)
        List<Long> hotShopIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L);
        for (Long id : hotShopIds) {
            shopServiceImpl.refreshCache(id, CACHE_SHOP_TTL);
        }
        log.info("===== 商铺缓存预热完成 =====");
    }
}
