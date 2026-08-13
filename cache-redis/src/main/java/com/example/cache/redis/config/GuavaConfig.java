package com.example.cache.redis.config;

import com.example.cache.redis.entity.User;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Guava local cache configuration.
 * </p>
 *
 * @author NamHoang
 */
@Configuration
public class GuavaConfig {

    /**
     * Local cache: at most 100 entries, expiring 60 seconds after write.
     */
    @Bean
    public Cache<Long, User> userLocalCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }
}
