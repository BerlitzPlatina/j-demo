package com.example.cache.redis.service.impl;

import com.example.cache.redis.entity.User;
import com.example.cache.redis.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * UserService
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-11-15 16:45
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    /**
     * Stand-in for a database
     */
    private static final Map<Long, User> DATABASES = new ConcurrentHashMap<>();

    /**
     * Seed data
     */
    static {
        DATABASES.put(1L, new User(1L, "user1"));
        DATABASES.put(2L, new User(2L, "user2"));
        DATABASES.put(3L, new User(3L, "user3"));
    }

    /**
     * Create or update a user.
     *
     * @param user the user
     * @return the saved user
     */
    @CachePut(value = "user", key = "#user.id")
    @Override
    public User saveOrUpdate(User user) {
        DATABASES.put(user.getId(), user);
        log.info("Saved user [user] = {}", user);
        return user;
    }

    /**
     * Look up a user.
     *
     * @param id the cache key
     * @return the user, or {@code null} if absent
     */
    @Cacheable(value = "user", key = "#id")
    @Override
    public User get(Long id) {
        // Pretend this reads from the database
        log.info("Loaded user [id] = {}", id);
        return DATABASES.get(id);
    }

    /**
     * Delete a user.
     *
     * @param id the cache key
     */
    @CacheEvict(value = "user", key = "#id")
    @Override
    public void delete(Long id) {
        DATABASES.remove(id);
        log.info("Deleted user [id] = {}", id);
    }
}
