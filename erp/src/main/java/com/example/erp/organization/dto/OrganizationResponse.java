package com.example.erp.organization.dto;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Response class for an organization. A record, so the entity itself never reaches Jackson.
 */
public record OrganizationResponse(
        Long id,
        String name,
        Boolean logoUploaded,
        Boolean defaultOrg,
        String userRole,
        LocalDate accountCreatedDate,
        String timeZone,
        String languageCode,
        String dateFormat,
        String fieldSeparator,
        Integer fiscalYearStartMonth,
        Boolean taxGroupEnabled,
        String userStatus,
        String contactName,
        String industryType,
        Date createTime,
        Date lastUpdateTime,
        List<AddressResponse> addresses) {
}
