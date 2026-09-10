package com.example.erp.designation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/designations/{id}}: a full replacement, so an omitted optional field is
 * written as null.
 */
public record DesignationUpdateRequest(
        @NotBlank @Size(max = 255) String name) {
}
