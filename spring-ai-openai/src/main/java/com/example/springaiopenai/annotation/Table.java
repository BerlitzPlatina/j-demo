package com.example.springaiopenai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Đánh dấu tên bảng của entity
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Table {
    /**
     * Tên bảng trong database
     *
     * @return tên bảng
     */
    String name();
}
