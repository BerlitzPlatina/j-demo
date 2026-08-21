package com.example.orm.jpa.exception;

import com.example.common.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The handlers shared through the {@code common-web} jar reach this application only via
 * {@link BlazeExceptionHandler}, the local {@code @RestControllerAdvice} subclass. Guards against
 * that subclass being dropped, which would silently leave the application with no advice at all.
 */
@SpringBootTest
class SharedExceptionHandlerTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void adviceFromTheSharedJarIsRegisteredExactlyOnce() {
        assertThat(context.getBeansOfType(GlobalExceptionHandler.class))
                .containsOnlyKeys("blazeExceptionHandler");
    }
}
