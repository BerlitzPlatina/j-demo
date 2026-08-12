package com.example.springaiopenai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Đánh dấu tên cột khi tên field khác tên cột
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Column {
    /**
     * Tên cột trong database
     *
     * @return tên cột
     */
    String name();
}
