package com.example.cache.redis.controller;

import com.example.cache.redis.entity.User;
import com.example.cache.redis.service.UserService;
import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Cache demo endpoints: Redis (distributed) + Guava (local).
 * </p>
 *
 * @author NamHoang
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final UserService userService;
    private final RedisTemplate<String, Serializable> redisCacheTemplate;
    private final Cache<Long, User> userLocalCache;

    // ---------------------------------------------------------------
    // Redis via @Cacheable / @CachePut / @CacheEvict annotations
    // ---------------------------------------------------------------

    /**
     * The first call hits the "database" and logs; later calls are served from Redis.
     */
    @GetMapping("/users/{id}")
    public User get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping("/users")
    public User saveOrUpdate(@RequestBody User user) {
        return userService.saveOrUpdate(user);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        userService.delete(id);
        userLocalCache.invalidate(id);
        return Map.of("id", id, "deleted", true);
    }

    // ---------------------------------------------------------------
    // Redis via RedisTemplate directly
    // ---------------------------------------------------------------

    @PostMapping("/redis/{key}")
    public Map<String, Object> redisSet(@PathVariable String key,
                                        @RequestParam String value,
                                        @RequestParam(defaultValue = "300") long ttlSeconds) {
        redisCacheTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        return Map.of("key", key, "value", value, "ttlSeconds", ttlSeconds);
    }

    @GetMapping("/redis/{key}")
    public Map<String, Object> redisGet(@PathVariable String key) {
        Serializable value = redisCacheTemplate.opsForValue().get(key);
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        result.put("hit", value != null);
        return result;
    }

    // ---------------------------------------------------------------
    // Guava local cache: only falls through to Redis / database on a miss
    // ---------------------------------------------------------------

    /**
     * Two-level cache: Guava (local) -> Redis -> database.
     */
    @GetMapping("/guava/users/{id}")
    public User getFromLocalCache(@PathVariable Long id) throws Exception {
        return userLocalCache.get(id, () -> {
            log.info("Guava cache miss, loading from source [id] = {}", id);
            return userService.get(id);
        });
    }

    /**
     * Guava cache statistics (hit rate, load count, ...).
     */
    @GetMapping("/guava/stats")
    public Map<String, Object> guavaStats() {
        return Map.of("size", userLocalCache.size(), "stats", userLocalCache.stats().toString());
    }

    @DeleteMapping("/guava/users/{id}")
    public Map<String, Object> evictLocalCache(@PathVariable Long id) {
        userLocalCache.invalidate(id);
        return Map.of("id", id, "evicted", true);
    }
}
