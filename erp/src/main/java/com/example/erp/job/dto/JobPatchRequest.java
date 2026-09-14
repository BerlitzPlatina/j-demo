package com.example.erp.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PATCH /api/jobs/{id}}: every field is optional and a null one means
 * "leave this column as it is".
 * <p>
 * The consequence, and the reason PUT exists alongside it, is that PATCH cannot clear a column
 * back to null - null is already spoken for.
 */
public record JobPatchRequest(
        Long legalEntityIdId,
        @Size(max = 32) String jobCode,
        @Size(max = 120) String jobName,
        @Size(max = 16) String jobStatus,
        @Size(max = 32) String referenceQuality,
        @Size(max = 32) String source,
        @Min(0) Integer createdBy,
        @Min(0) Integer updatedBy) {
}
