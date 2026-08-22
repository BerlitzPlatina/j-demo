package com.example.erp.exception;

import com.example.common.web.exception.GlobalExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Registers the shared handlers from {@link GlobalExceptionHandler} in this application.
 * <p>
 * The base class lives in the {@code common-web} jar, outside this application's component-scan
 * root, so it is this subclass - inside the scanned package - that Spring picks up.
 * <p>
 * One advice for the whole application, not one per feature package: a second
 * {@code @RestControllerAdvice} mapping the same exception would make which one wins depend on
 * bean ordering. Anything specific to a single feature belongs in that feature's service, thrown
 * as an {@code IllegalArgumentException} with a message naming the field.
 */
@RestControllerAdvice
public class ErpExceptionHandler extends GlobalExceptionHandler {

    @Override
    protected String dataIntegrityMessage(DataIntegrityViolationException ex) {
        return "Request conflicts with an existing record: either a unique value is already taken, "
                + "or the row is still referenced by another one";
    }
}
