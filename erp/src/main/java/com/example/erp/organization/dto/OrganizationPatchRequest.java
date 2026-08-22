package com.example.erp.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for {@code PATCH /api/organizations/{id}}: every field is optional and a null one means
 * "leave this column as it is".
 * <p>
 * The consequence, and the reason PUT exists alongside it, is that PATCH cannot clear a column
 * back to null - null is already spoken for.
 */
public record OrganizationPatchRequest(
        @Size(max = 255) String name,
        Boolean logoUploaded,
        Boolean defaultOrg,
        @Size(max = 100) String userRole,
        LocalDate accountCreatedDate,
        @Size(max = 50) String timeZone,
        @Size(max = 10) String languageCode,
        @Size(max = 50) String dateFormat,
        @Size(max = 10) String fieldSeparator,
        @Min(1) @Max(12) Integer fiscalYearStartMonth,
        Boolean taxGroupEnabled,
        @Size(max = 50) String userStatus,
        @Size(max = 255) String contactName,
        @Size(max = 100) String industryType) {
}
