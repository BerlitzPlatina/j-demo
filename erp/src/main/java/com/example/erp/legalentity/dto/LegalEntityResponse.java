package com.example.erp.legalentity.dto;

import java.util.Date;

/**
 * Response class for a LegalEntity. A record, so the entity itself never reaches Jackson.
 */
public record LegalEntityResponse(
        Long id,
        String entityCode,
        String registeredName,
        String registrationNumber,
        String taxRegistrationNumber,
        String countryCode,
        String baseCurrency,
        String entityStatus,
        Boolean legacyDefault,
        String source,
        Integer createdBy,
        Integer updatedBy,
        Date createTime,
        Date lastUpdateTime) {
}
