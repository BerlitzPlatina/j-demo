package com.example.orm.jpa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for {@code PUT /api/users/{id}}. A null {@code departmentIds} leaves the
 * existing assignment untouched; an empty list clears it.
 */
public record UserUpdateRequest(
        @NotBlank @Size(max = 32) String name,
        @NotBlank @Email @Size(max = 32) String email,
        @NotBlank @Pattern(regexp = "\\d{9,15}", message = "phoneNumber must be 9-15 digits") String phoneNumber,
        Integer status,
        List<Long> departmentIds) {
}
