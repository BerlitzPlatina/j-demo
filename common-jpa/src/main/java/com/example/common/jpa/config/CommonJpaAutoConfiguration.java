package com.example.common.jpa.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * <p>
 * Turns on JPA auditing for every module that depends on common-jpa, so
 * {@link com.example.common.jpa.entity.AbstractAuditModel} gets its createTime /
 * lastUpdateTime populated without each application repeating the annotation.
 * </p>
 *
 * <p>
 * Registered through META-INF/spring/...AutoConfiguration.imports rather than
 * component scanning: this package sits outside the applications' scan root.
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass({ EntityManagerFactory.class, AuditingEntityListener.class })
@EnableJpaAuditing
public class CommonJpaAutoConfiguration {
}
