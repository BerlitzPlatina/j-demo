package com.example.task.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>
 * Turns on scheduling. Without {@code @EnableScheduling} every {@code @Scheduled} method is
 * simply ignored — no error, the jobs just never run.
 * </p>
 * <p>
 * The thread pool is not built here: Spring Boot already provides a scheduler, and its size is
 * set with {@code spring.task.scheduling.pool.size} in application.properties. That matters
 * because the default pool holds a single thread, so a slow job would delay every other job.
 * </p>
 *
 * @author NamHoang
 */
@Configuration
@EnableScheduling
public class TaskConfig {
}
