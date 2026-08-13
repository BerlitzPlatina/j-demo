package com.example.cache.redis.service;

import com.example.cache.redis.entity.User;

/**
 * <p>
 * UserService
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-11-15 16:45
 */
public interface UserService {
    /**
     * Create or update a user.
     *
     * @param user the user
     * @return the saved user
     */
    User saveOrUpdate(User user);

    /**
     * Look up a user.
     *
     * @param id the cache key
     * @return the user, or {@code null} if absent
     */
    User get(Long id);

    /**
     * Delete a user.
     *
     * @param id the cache key
     */
    void delete(Long id);
}
