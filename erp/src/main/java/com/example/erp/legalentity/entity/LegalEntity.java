package com.example.erp.legalentity.entity;

import com.example.common.jpa.entity.AbstractAuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The {@code legal_entities} table.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}, so they are not
 * repeated here.
 */
@Entity
@Table(name = "legal_entities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LegalEntity extends AbstractAuditModel {

    @Column(name = "entity_code", nullable = false, unique = true)
    private String entityCode;

    @Column(name = "registered_name", nullable = false)
    private String registeredName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "tax_registration_number")
    private String taxRegistrationNumber;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    @Column(name = "entity_status", nullable = false)
    private String entityStatus;

    @Column(name = "legacy_default", unique = true)
    private Boolean legacyDefault;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "updated_by")
    private Integer updatedBy;
}
