package com.example.erp.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * Response class for a Tax. A record, so the entity itself never reaches
 * Jackson.
 */
public record TaxResponse(
                Long id,
                String name,
                String displayName,
                BigDecimal percentage,
                String type,
                String factor,
                String specificType,
                String authorityName,
                String authorityId,
                Boolean isValueAdded,
                Long taxAccountId,
                Long purchaseTaxAccountId,
                Long purchaseTaxExpenseAccountId,
                Long tdsPayableAccountId,
                Long tcsReceivableAccountId,
                Long tcsPayableAccountId,
                String accountTracking,
                Boolean isNonAdvolTax,
                Boolean isStateCess,
                String diffRateReason,
                LocalDate startDate,
                LocalDate endDate,
                String description,
                Date createTime,
                Date lastUpdateTime) {
}
