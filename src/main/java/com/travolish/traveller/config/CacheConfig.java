package com.travolish.traveller.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableAsync
public class CacheConfig {

    /**
     * Per-cache Caffeine configuration — each cache has its own TTL and size cap.
     *
     * <pre>
     *  Cache name                  Max entries  TTL      Rationale
     *  ─────────────────────────── ─────────── ─────    ──────────────────────────────────
     *  hotels                         500       5 min    Per-id hotel entity (detail page)
     *  hotel-search                   200       5 min    Search result pages
     *  notification-templates         100      10 min    Rarely change; longer TTL safe
     * </pre>
     *
     * Using {@link SimpleCacheManager} instead of {@link org.springframework.cache.caffeine.CaffeineCacheManager}
     * so each cache gets its own {@link Caffeine} spec rather than sharing one.
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            buildCache("hotels",                 500, 5,  TimeUnit.MINUTES),
            buildCache("hotel-search",           200, 5,  TimeUnit.MINUTES),
            buildCache("notification-templates", 100, 10, TimeUnit.MINUTES)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, int maxSize, int ttl, TimeUnit unit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl, unit)
                .recordStats()
                .build());
    }

    /**
     * Bounded executor for @Async("notificationExecutor").
     * Default Spring executor uses an unbounded queue that can grow without limit.
     */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
