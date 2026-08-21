package com.example.orm.jpa.exception;

import com.example.common.web.exception.GlobalExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Registers the shared handlers from {@link GlobalExceptionHandler} in this application.
 * <p>
 * The base class lives in the {@code common-web} jar, outside this application's component-scan
 * root, so it is this subclass - inside the scanned package - that Spring picks up. Every
 * {@code @ExceptionHandler} mapping is inherited; only the message that is specific to this
 * module's schema is overridden.
 */
@RestControllerAdvice
public class OrmJpaExceptionHandler extends GlobalExceptionHandler {

    @Override
    protected String dataIntegrityMessage(DataIntegrityViolationException ex) {
        return "name, email and phoneNumber must be unique";
    }
}
