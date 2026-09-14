package com.example.erp.legalentity.mapper;

import com.example.erp.legalentity.dto.LegalEntityCreateRequest;
import com.example.erp.legalentity.dto.LegalEntityPatchRequest;
import com.example.erp.legalentity.dto.LegalEntityResponse;
import com.example.erp.legalentity.dto.LegalEntityUpdateRequest;
import com.example.erp.legalentity.entity.LegalEntity;

/**
 * Entity to response, and request to entity. Kept in one place so no controller ever serializes
 * an entity and no service hand-copies fields.
 */
public final class LegalEntityMapper {

    private LegalEntityMapper() {
    }

    public static LegalEntityResponse toResponse(LegalEntity legalEntity) {
        return new LegalEntityResponse(
                legalEntity.getId(),
                legalEntity.getEntityCode(),
                legalEntity.getRegisteredName(),
                legalEntity.getRegistrationNumber(),
                legalEntity.getTaxRegistrationNumber(),
                legalEntity.getCountryCode(),
                legalEntity.getBaseCurrency(),
                legalEntity.getEntityStatus(),
                legalEntity.getLegacyDefault(),
                legalEntity.getSource(),
                legalEntity.getCreatedBy(),
                legalEntity.getUpdatedBy(),
                legalEntity.getCreateTime(),
                legalEntity.getLastUpdateTime());
    }

    public static LegalEntity toEntity(LegalEntityCreateRequest request) {
        return LegalEntity.builder()
                .entityCode(request.entityCode())
                .registeredName(request.registeredName())
                .registrationNumber(request.registrationNumber())
                .taxRegistrationNumber(request.taxRegistrationNumber())
                .countryCode(request.countryCode())
                .baseCurrency(request.baseCurrency())
                .entityStatus(request.entityStatus())
                .legacyDefault(request.legacyDefault())
                .source(request.source())
                .createdBy(request.createdBy())
                .updatedBy(request.updatedBy())
                .build();
    }

    /** Full replacement: an omitted optional field is written as null. */
    public static void replace(LegalEntity target, LegalEntityUpdateRequest request) {
        target.setEntityCode(request.entityCode());
        target.setRegisteredName(request.registeredName());
        target.setRegistrationNumber(request.registrationNumber());
        target.setTaxRegistrationNumber(request.taxRegistrationNumber());
        target.setCountryCode(request.countryCode());
        target.setBaseCurrency(request.baseCurrency());
        target.setEntityStatus(request.entityStatus());
        target.setLegacyDefault(request.legacyDefault());
        target.setSource(request.source());
        target.setCreatedBy(request.createdBy());
        target.setUpdatedBy(request.updatedBy());
    }

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge(LegalEntity target, LegalEntityPatchRequest request) {
        if (request.entityCode() != null) {
            target.setEntityCode(request.entityCode());
        }
        if (request.registeredName() != null) {
            target.setRegisteredName(request.registeredName());
        }
        if (request.registrationNumber() != null) {
            target.setRegistrationNumber(request.registrationNumber());
        }
        if (request.taxRegistrationNumber() != null) {
            target.setTaxRegistrationNumber(request.taxRegistrationNumber());
        }
        if (request.countryCode() != null) {
            target.setCountryCode(request.countryCode());
        }
        if (request.baseCurrency() != null) {
            target.setBaseCurrency(request.baseCurrency());
        }
        if (request.entityStatus() != null) {
            target.setEntityStatus(request.entityStatus());
        }
        if (request.legacyDefault() != null) {
            target.setLegacyDefault(request.legacyDefault());
        }
        if (request.source() != null) {
            target.setSource(request.source());
        }
        if (request.createdBy() != null) {
            target.setCreatedBy(request.createdBy());
        }
        if (request.updatedBy() != null) {
            target.setUpdatedBy(request.updatedBy());
        }
    }
}
