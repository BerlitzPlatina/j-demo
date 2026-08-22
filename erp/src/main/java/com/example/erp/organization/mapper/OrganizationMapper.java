package com.example.erp.organization.mapper;

import com.example.erp.organization.dto.OrganizationCreateRequest;
import com.example.erp.organization.dto.OrganizationPatchRequest;
import com.example.erp.organization.dto.OrganizationResponse;
import com.example.erp.organization.dto.OrganizationUpdateRequest;
import com.example.erp.organization.entity.Organization;

/**
 * Entity to response, and request to entity. Kept in one place so no controller ever serializes
 * an entity and no service hand-copies fields.
 */
public final class OrganizationMapper {

    private OrganizationMapper() {
    }

    public static OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getLogoUploaded(),
                organization.getDefaultOrg(),
                organization.getUserRole(),
                organization.getAccountCreatedDate(),
                organization.getTimeZone(),
                organization.getLanguageCode(),
                organization.getDateFormat(),
                organization.getFieldSeparator(),
                organization.getFiscalYearStartMonth(),
                organization.getTaxGroupEnabled(),
                organization.getUserStatus(),
                organization.getContactName(),
                organization.getIndustryType(),
                organization.getCreateTime(),
                organization.getLastUpdateTime());
    }

    /** The NOT NULL flags fall back to false so an omitted field is not a constraint violation. */
    public static Organization toEntity(OrganizationCreateRequest request) {
        return Organization.builder()
                .name(request.name())
                .logoUploaded(orFalse(request.logoUploaded()))
                .defaultOrg(orFalse(request.defaultOrg()))
                .userRole(request.userRole())
                .accountCreatedDate(request.accountCreatedDate())
                .timeZone(request.timeZone())
                .languageCode(request.languageCode())
                .dateFormat(request.dateFormat())
                .fieldSeparator(request.fieldSeparator())
                .fiscalYearStartMonth(request.fiscalYearStartMonth())
                .taxGroupEnabled(orFalse(request.taxGroupEnabled()))
                .userStatus(request.userStatus())
                .contactName(request.contactName())
                .industryType(request.industryType())
                .build();
    }

    /** Full replacement: an omitted optional field is written as null. */
    public static void replace(Organization target, OrganizationUpdateRequest request) {
        target.setName(request.name());
        target.setLogoUploaded(orFalse(request.logoUploaded()));
        target.setDefaultOrg(orFalse(request.defaultOrg()));
        target.setUserRole(request.userRole());
        target.setAccountCreatedDate(request.accountCreatedDate());
        target.setTimeZone(request.timeZone());
        target.setLanguageCode(request.languageCode());
        target.setDateFormat(request.dateFormat());
        target.setFieldSeparator(request.fieldSeparator());
        target.setFiscalYearStartMonth(request.fiscalYearStartMonth());
        target.setTaxGroupEnabled(orFalse(request.taxGroupEnabled()));
        target.setUserStatus(request.userStatus());
        target.setContactName(request.contactName());
        target.setIndustryType(request.industryType());
    }

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge(Organization target, OrganizationPatchRequest request) {
        if (request.name() != null) {
            target.setName(request.name());
        }
        if (request.logoUploaded() != null) {
            target.setLogoUploaded(request.logoUploaded());
        }
        if (request.defaultOrg() != null) {
            target.setDefaultOrg(request.defaultOrg());
        }
        if (request.userRole() != null) {
            target.setUserRole(request.userRole());
        }
        if (request.accountCreatedDate() != null) {
            target.setAccountCreatedDate(request.accountCreatedDate());
        }
        if (request.timeZone() != null) {
            target.setTimeZone(request.timeZone());
        }
        if (request.languageCode() != null) {
            target.setLanguageCode(request.languageCode());
        }
        if (request.dateFormat() != null) {
            target.setDateFormat(request.dateFormat());
        }
        if (request.fieldSeparator() != null) {
            target.setFieldSeparator(request.fieldSeparator());
        }
        if (request.fiscalYearStartMonth() != null) {
            target.setFiscalYearStartMonth(request.fiscalYearStartMonth());
        }
        if (request.taxGroupEnabled() != null) {
            target.setTaxGroupEnabled(request.taxGroupEnabled());
        }
        if (request.userStatus() != null) {
            target.setUserStatus(request.userStatus());
        }
        if (request.contactName() != null) {
            target.setContactName(request.contactName());
        }
        if (request.industryType() != null) {
            target.setIndustryType(request.industryType());
        }
    }

    private static boolean orFalse(Boolean value) {
        return value != null && value;
    }
}
