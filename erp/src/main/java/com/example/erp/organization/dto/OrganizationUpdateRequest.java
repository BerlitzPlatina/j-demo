package com.example.erp.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for {@code PUT /api/organizations/{id}} - a full replacement, so every optional field
 * left out is written as null. Use {@code PATCH} for a partial change.
 */
public record OrganizationUpdateRequest(
        @NotBlank @Size(max = 255) String name,
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
