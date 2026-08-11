package com.example.exception_handler.constant;

import lombok.Getter;

@Getter
public enum Status {
    OK(200, "Operation successful."),

    UNKNOWN_ERROR(500, "There was a server error.");

    private Integer code;

    private String message;

    Status(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
