package com.example.common.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

/**
 * Standard envelope for every REST response.
 *
 * @param code    business/HTTP status code
 * @param message human readable message
 * @param data    payload, omitted from JSON when null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "Success", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), "Created", data);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, null);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return new ApiResponse<>(status.value(), message, null);
    }
}
