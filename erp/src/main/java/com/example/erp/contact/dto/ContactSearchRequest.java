package com.example.erp.contact.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContactSearchRequest(
                Long organizationId,
                String contactName,
                String companyName,
                String keyword,
                String contactType,
                String customerSubType,
                String status,
                Boolean hasTransaction,
                BigDecimal creditLimitFrom,
                BigDecimal creditLimitTo,
                LocalDate createdFrom,
                LocalDate createdTo) {
}
