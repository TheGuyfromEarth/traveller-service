package com.travolish.traveller.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache names and their intended TTLs:
     *
     *  hotel-search  — public hotel search results (5 min)
     *                  Evicted on any hotel create / update / delete.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("hotel-search");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(500)           // at most 500 distinct search-parameter combinations
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()              // enables hit/miss metrics via Actuator if added later
        );
        return manager;
    }
}
