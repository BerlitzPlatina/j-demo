package com.example.erp.organization.dto;

/**
 * One address of an organization, as nested in {@link OrganizationResponse}.
 */
public record AddressResponse(
        Long id,
        String streetAddress1,
        String streetAddress2,
        String city,
        String state,
        String country,
        String zip) {
}
