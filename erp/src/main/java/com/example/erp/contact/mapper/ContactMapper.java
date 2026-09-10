package com.example.erp.contact.mapper;

import com.example.erp.contact.dto.ContactCreateRequest;
import com.example.erp.contact.dto.ContactOrganizationResponse;
import com.example.erp.contact.dto.ContactResponse;
import com.example.erp.contact.entity.Contact;
import com.example.erp.organization.entity.Organization;

/**
 * Entity to response. Kept in one place so no controller ever serializes an
 * entity and no
 * service hand-copies fields.
 * <p>
 * Only the read direction exists so far: the contact module has no
 * create/update/patch request
 * yet. The request-to-entity methods belong here as well once it does - see
 * {@code OrganizationMapper} for the shape ({@code toEntity} / {@code replace}
 * / {@code merge}).
 */
public final class ContactMapper {

    private ContactMapper() {
    }

    /**
     * Positional constructor, so the order below must stay in step with
     * {@link ContactResponse}. That is the price of a record: the compiler catches
     * a wrong
     * <em>type</em> but not two swapped fields of the same type, which is why the
     * order here
     * follows the entity's own declaration order.
     */
    public static ContactResponse toResponse(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getContactNumber(),
                contact.getContactName(),
                contact.getCompanyName(),
                contact.getContactType(),
                contact.getStatus(),
                contact.getCurrencyCode(),
                contact.getPaymentTerms(),
                contact.getOutstandingReceivableAmount(),
                contact.getUnusedCreditsReceivableAmount(),
                contact.getOwnerId(),
                toOrganizationResponse(contact.getOrganization()));
    }

    /**
     * Null only when the contact carries no organization_id. Reading the
     * association here resolves a lazy proxy, which is why {@code Organization} is
     * {@code @BatchSize}d: the first row triggers one select that also loads the
     * organizations of the rest of the page. It does mean this runs inside the
     * service's transaction - a detached contact would throw here instead.
     */
    private static ContactOrganizationResponse toOrganizationResponse(Organization organization) {
        if (organization == null) {
            return null;
        }
        return new ContactOrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getContactName(),
                organization.getIndustryType());
    }

    public static Contact toEntity(ContactCreateRequest request, Long organizationId) {
        return Contact.builder().contactName(request.contactName())
                .hasTransaction(true)
                .paymentReminderEnabled(true)
                .isTaxable(true)
                .isTdsRegistered(true)
                .isLinkedWithCrm(true)
                .organizationId(organizationId)
                .contactType(request.contactType())
                .status(request.status())
                .build();
    }
}
