package com.example.rbac.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * JWT 配置
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-07 13:42
 */
@ConfigurationProperties(prefix = "jwt.config")
@Data
public class JwtConfig {
    /**
     * jwt 加密 key.
     * <p>
     * HS256 needs at least 256 bits of key material, and jjwt 0.12 rejects anything shorter, so
     * this default is 32 ASCII characters. It is a placeholder: override jwt.config.key with a
     * secret of your own for anything beyond local development.
     */
    private String key = "example-dev-only-jwt-signing-key";

    /**
     * jwt 过期时间，默认值：600000 {@code 10 分钟}.
     */
    private Long ttl = 600000L;

    /**
     * 开启 记住我 之后 jwt 过期时间，默认值 604800000 {@code 7 天}
     */
    private Long remember = 604800000L;
}
