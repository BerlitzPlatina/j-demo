package com.example.orm.jpa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for {@code POST /api/users}.
 */
public record UserCreateRequest(
        @NotBlank @Size(max = 32) String name,
        @NotBlank @Size(min = 6, max = 32) String password,
        @NotBlank @Email @Size(max = 32) String email,
        @NotBlank @Pattern(regexp = "\\d{9,15}", message = "phoneNumber must be 9-15 digits") String phoneNumber,
        Integer status,
        List<Long> departmentIds) {
}
