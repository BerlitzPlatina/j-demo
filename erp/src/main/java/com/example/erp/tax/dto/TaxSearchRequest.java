package com.example.erp.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Query parameters of {@code GET /api/settings/taxes}. Every field is optional: one left out
 * is one filter fewer in the SQL.
 * <p>
 * The {@code ...From}/{@code ...To} pairs are inclusive ranges, and either end may stand alone.
 */
public record TaxSearchRequest(
        /** One search box, matched against the name, display name, authority and description. */
        String keyword,
        String name,
        String displayName,
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
        BigDecimal percentageFrom,
        BigDecimal percentageTo,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        LocalDate endDateFrom,
        LocalDate endDateTo,
        LocalDate createdFrom,
        LocalDate createdTo) {
}
