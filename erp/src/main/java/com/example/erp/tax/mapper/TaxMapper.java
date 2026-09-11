package com.example.erp.tax.mapper;

import com.example.erp.tax.dto.TaxCreateRequest;
import com.example.erp.tax.dto.TaxPatchRequest;
import com.example.erp.tax.dto.TaxResponse;
import com.example.erp.tax.dto.TaxUpdateRequest;
import com.example.erp.tax.entity.Tax;

/**
 * Entity to response, and request to entity. Kept in one place so no controller
 * ever serializes
 * an entity and no service hand-copies fields.
 */
public final class TaxMapper {

    private TaxMapper() {
    }

    public static TaxResponse toResponse(Tax tax) {
        return new TaxResponse(
                tax.getId(),
                tax.getName(),
                tax.getDisplayName(),
                tax.getPercentage(),
                tax.getType(),
                tax.getFactor(),
                tax.getSpecificType(),
                tax.getAuthorityName(),
                tax.getAuthorityId(),
                tax.getIsValueAdded(),
                tax.getTaxAccountId(),
                tax.getPurchaseTaxAccountId(),
                tax.getPurchaseTaxExpenseAccountId(),
                tax.getTdsPayableAccountId(),
                tax.getTcsReceivableAccountId(),
                tax.getTcsPayableAccountId(),
                tax.getAccountTracking(),
                tax.getIsNonAdvolTax(),
                tax.getIsStateCess(),
                tax.getDiffRateReason(),
                tax.getStartDate(),
                tax.getEndDate(),
                tax.getDescription(),
                tax.getCreateTime(),
                tax.getLastUpdateTime());
    }

    public static Tax toEntity(TaxCreateRequest request) {
        return Tax.builder()
                .name(request.name())
                .build();
    }

    /** Full replacement: an omitted optional field is written as null. */
    public static void replace(Tax target, TaxUpdateRequest request) {
        target.setName(request.name());
    }

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge(Tax target, TaxPatchRequest request) {
        if (request.name() != null) {
            target.setName(request.name());
        }
    }
}
