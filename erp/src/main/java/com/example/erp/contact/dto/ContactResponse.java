package com.example.erp.contact.dto;

import java.math.BigDecimal;

/**
 * Response class for a contact. A record, so the entity itself never reaches
 * Jackson.
 * <p>
 * The field names follow {@link com.example.erp.contact.entity.Contact} -
 * camelCase, like the organization module - while the underlying columns stay
 * snake_case.
 * <p>
 * Mirrors the mapped columns only. {@code is_portal_enabled} exists in the
 * {@code contacts} table
 * but is not a field of the entity, so it cannot be answered from here; the
 * child tables
 * (contact_persons, contact_addresses, ...) have no entities yet, so they are
 * absent too.
 */
public record ContactResponse(
                Long id,
                String contactNumber,
                String contactName,
                String companyName,
                String contactType,
                String status,
                String currencyCode,
                Integer paymentTerms,
                BigDecimal outstandingReceivableAmount,
                BigDecimal unusedCreditsReceivableAmount,
                Long ownerId) {
}
