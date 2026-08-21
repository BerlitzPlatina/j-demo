package com.example.common.web.exception;

import com.example.common.web.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Turns exceptions into the same {@link ApiResponse} envelope used by the controllers.
 * <p>
 * Deliberately <strong>not</strong> annotated with {@code @RestControllerAdvice}: this class lives in
 * a jar, outside the component-scan root of the applications that depend on it, so annotating it
 * here would achieve nothing. Each application declares its own advice extending this class, which
 * its own scan does pick up:
 * <pre>
 * &#64;RestControllerAdvice
 * public class MyExceptionHandler extends GlobalExceptionHandler { }
 * </pre>
 * Spring discovers {@code @ExceptionHandler} methods through the bean's class hierarchy, so the
 * subclass inherits every mapping below and can override the hooks to specialise a message.
 * This is the same shape as Spring's own {@code ResponseEntityExceptionHandler}.
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBody(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + defaultMessage(error))
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /** Violated unique constraints surface here. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, dataIntegrityMessage(ex)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    /**
     * Message returned for a constraint violation. The generic default says nothing about the
     * schema; override it to name the columns that are actually unique in your module.
     */
    protected String dataIntegrityMessage(DataIntegrityViolationException ex) {
        return "Request conflicts with an existing record";
    }

    private static String defaultMessage(FieldError error) {
        return error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
    }
}
