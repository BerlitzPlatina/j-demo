package com.example.orm.jpa.exception;

import com.example.common.web.exception.GlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Registers the shared handlers from {@link GlobalExceptionHandler} in this application.
 * No mapping is specialised here - see the base class for the reason this subclass exists.
 */
@RestControllerAdvice
public class BlazeExceptionHandler extends GlobalExceptionHandler {
}
