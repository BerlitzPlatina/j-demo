package com.example.springaiopenai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Đánh dấu khoá chính
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Pk {
    /**
     * Khoá chính tự tăng thì không đưa vào câu INSERT/UPDATE
     *
     * @return true nếu là khoá tự tăng
     */
    boolean auto() default true;
}
