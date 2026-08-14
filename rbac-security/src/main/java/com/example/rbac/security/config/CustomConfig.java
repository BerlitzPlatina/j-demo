package com.example.rbac.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 自定义配置
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-13 10:56
 */
@ConfigurationProperties(prefix = "custom.config")
@Data
public class CustomConfig {
    /**
     * 不需要拦截的地址
     * <p>
     * Initialised so the application still starts when custom.config.ignores is absent from the
     * configuration; IgnoreConfig defaults every list to empty.
     */
    private IgnoreConfig ignores = new IgnoreConfig();
}
