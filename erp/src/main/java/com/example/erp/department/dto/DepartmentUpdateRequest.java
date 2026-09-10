package com.example.erp.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/departments/{id}}: a full replacement, so an omitted optional field is
 * written as null.
 */
public record DepartmentUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String description,
        @NotBlank @Size(max = 255) String departmentCode) {
}
