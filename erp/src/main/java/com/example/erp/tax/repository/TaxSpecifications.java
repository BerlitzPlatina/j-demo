package com.example.erp.tax.repository;

import org.springframework.data.jpa.domain.Specification;

import com.example.erp.common.constant.ErpConstants;
import com.example.erp.common.specification.SpecBuilder;
import com.example.erp.tax.dto.TaxSearchRequest;
import com.example.erp.tax.entity.Tax;

/**
 * The {@code where} of the tax search: optional filters only, so the whole class is the list
 * of which request field maps onto which column - {@link SpecBuilder} handles dropping the
 * ones the request left out.
 */
public final class TaxSpecifications {

    private TaxSpecifications() {
    }

    public static Specification<Tax> from(TaxSearchRequest request) {
        return SpecBuilder.<Tax>of()
                .likeAny(request.keyword(), "name", "displayName", "authorityName", "description")
                .like("name", request.name())
                .like("displayName", request.displayName())
                .like("authorityName", request.authorityName())
                .eq("type", request.type())
                .eq("factor", request.factor())
                .eq("specificType", request.specificType())
                .eq("authorityId", request.authorityId())
                .eq("isValueAdded", request.isValueAdded())
                .eq("taxAccountId", request.taxAccountId())
                .eq("purchaseTaxAccountId", request.purchaseTaxAccountId())
                .eq("purchaseTaxExpenseAccountId", request.purchaseTaxExpenseAccountId())
                .eq("tdsPayableAccountId", request.tdsPayableAccountId())
                .eq("tcsReceivableAccountId", request.tcsReceivableAccountId())
                .eq("tcsPayableAccountId", request.tcsPayableAccountId())
                .eq("accountTracking", request.accountTracking())
                .eq("isNonAdvolTax", request.isNonAdvolTax())
                .eq("isStateCess", request.isStateCess())
                .eq("diffRateReason", request.diffRateReason())
                .between("percentage", request.percentageFrom(), request.percentageTo())
                .between("startDate", request.startDateFrom(), request.startDateTo())
                .between("endDate", request.endDateFrom(), request.endDateTo())
                .dayRange(ErpConstants.CREATE_TIME, request.createdFrom(), request.createdTo())
                .build();
    }
}
