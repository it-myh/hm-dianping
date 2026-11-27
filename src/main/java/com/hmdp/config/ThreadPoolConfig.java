package com.hmdp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类
 * 用于创建缓存刷新专用线程池，适配Redis逻辑过期方案异步更新缓存任务
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 缓存刷新线程池
     * 用途：商铺缓存逻辑过期时，异步提交数据库查询、缓存更新任务
     * @Bean("cacheRefreshPool") 指定bean名称，注入时通过名字匹配，防止和其他线程池冲突
     * @return 线程池实例
     */
    @Bean("cacheRefreshPool")
    public ExecutorService cacheRefreshPool() {
        return new ThreadPoolExecutor(
                3,                     // corePoolSize：核心线程数，常驻线程，IO型缓存更新任务3个足够
                6,                     // maximumPoolSize：最大线程数，任务激增时最多扩容到6个线程
                10L,                   // keepAliveTime：非核心线程空闲存活时间
                TimeUnit.SECONDS,      // 时间单位：秒，空闲超过10s，回收多余非核心线程
                new LinkedBlockingQueue<>(200), // 有界阻塞队列：最多存放200个等待中的更新任务，防止无界队列OOM
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：队列满了，由调用线程自己执行任务，不丢任务、不抛异常
        );
    }
}