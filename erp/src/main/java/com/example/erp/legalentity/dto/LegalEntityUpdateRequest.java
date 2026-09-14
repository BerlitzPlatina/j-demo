package com.example.erp.legalentity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/legal-entities/{id}}: a full replacement, so an omitted optional field is
 * written as null.
 */
public record LegalEntityUpdateRequest(
        @NotBlank @Size(max = 32) String entityCode,
        @NotBlank @Size(max = 100) String registeredName,
        @Size(max = 55) String registrationNumber,
        @Size(max = 55) String taxRegistrationNumber,
        @Size(max = 2) String countryCode,
        @NotBlank @Size(max = 3) String baseCurrency,
        @NotBlank @Size(max = 16) String entityStatus,
        Boolean legacyDefault,
        @NotBlank @Size(max = 32) String source,
        @NotNull @Min(0) @Max(65535) Integer createdBy,
        @Min(0) @Max(65535) Integer updatedBy) {
}
