package com.example.erp.tax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/taxes/{id}}: a full replacement, so an omitted optional field is
 * written as null.
 */
public record TaxUpdateRequest(
        @NotBlank @Size(max = 255) String name) {
}
