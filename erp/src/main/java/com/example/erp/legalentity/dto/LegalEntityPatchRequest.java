package com.example.erp.legalentity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PATCH /api/legal-entities/{id}}: every field is optional and a null one means
 * "leave this column as it is".
 * <p>
 * The consequence, and the reason PUT exists alongside it, is that PATCH cannot clear a column
 * back to null - null is already spoken for.
 */
public record LegalEntityPatchRequest(
        @Size(max = 32) String entityCode,
        @Size(max = 100) String registeredName,
        @Size(max = 55) String registrationNumber,
        @Size(max = 55) String taxRegistrationNumber,
        @Size(max = 2) String countryCode,
        @Size(max = 3) String baseCurrency,
        @Size(max = 16) String entityStatus,
        Boolean legacyDefault,
        @Size(max = 32) String source,
        @Min(0) @Max(65535) Integer createdBy,
        @Min(0) @Max(65535) Integer updatedBy) {
}
