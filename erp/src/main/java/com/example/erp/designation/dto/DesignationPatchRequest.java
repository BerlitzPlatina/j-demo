package com.example.erp.designation.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PATCH /api/designations/{id}}: every field is optional and a null one means
 * "leave this column as it is".
 * <p>
 * The consequence, and the reason PUT exists alongside it, is that PATCH cannot clear a column
 * back to null - null is already spoken for.
 */
public record DesignationPatchRequest(
        @Size(max = 255) String name) {
}
