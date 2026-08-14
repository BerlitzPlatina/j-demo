package com.example.rbac.security.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.io.Serializable;

/**
 * <p>
 * redis配置
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-11 15:16
 */
@Configuration
@AutoConfigureAfter(DataRedisAutoConfiguration.class)
@EnableCaching
public class RedisConfig {

    /**
     * 默认情况下的模板只能支持RedisTemplate<String, String>，也就是只能存入字符串，因此支持序列化
     */
    @Bean
    public RedisTemplate<String, Serializable> redisCacheTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    /**
     * Cache manager for the caching annotations. Spring Boot 4 moved cache auto-configuration into
     * its own starter, so without this bean {@code @EnableCaching} above fails at startup with
     * "a component required a bean of type CacheManager".
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()));

        return RedisCacheManager.builder(factory).cacheDefaults(config).build();
    }

    /**
     * JSON serializer (Jackson 3, which is what Spring Boot 4 ships). Type information is written
     * so values come back as their original type, and polymorphic deserialization is restricted to
     * this application's own types.
     */
    private GenericJacksonJsonRedisSerializer jsonSerializer() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.rbac.security.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .build();

        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .enableSpringCacheNullValueSupport()
                .build();
    }
}
