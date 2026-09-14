package com.example.erp.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/jobs/{id}}: a full replacement, so an omitted optional field is
 * written as null.
 */
public record JobUpdateRequest(
        @NotNull Long legalEntityIdId,
        @NotBlank @Size(max = 32) String jobCode,
        @NotBlank @Size(max = 120) String jobName,
        @NotBlank @Size(max = 16) String jobStatus,
        @NotBlank @Size(max = 32) String referenceQuality,
        @NotBlank @Size(max = 32) String source,
        @NotNull @Min(0) Integer createdBy,
        @Min(0) Integer updatedBy) {
}
