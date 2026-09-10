package com.example.erp.contact.dto;

/**
 * The organization a contact belongs to, as it appears inside
 * {@link ContactResponse}.
 * <p>
 * Deliberately not {@code OrganizationResponse}: that record carries the
 * address list, which would drag a second lazy collection into every contact
 * row. A contact list only needs enough to label and link the organization.
 */
public record ContactOrganizationResponse(
                Long id,
                String name,
                String contactName,
                String industryType) {
}
