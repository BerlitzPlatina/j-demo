package com.example.erp.contact.entity;

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

import java.math.BigDecimal;

@Entity
@Table(name = "contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Contact extends AbstractAuditModel {

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "contact_name", length = 255, nullable = false)
    private String contactName;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "contact_type", length = 20, nullable = false)
    private String contactType;

    @Column(name = "customer_sub_type", length = 20)
    private String customerSubType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "has_transaction", nullable = false)
    private Boolean hasTransaction;

    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit;

    @Column(name = "payment_reminder_enabled", nullable = false)
    private Boolean paymentReminderEnabled;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "is_taxable", nullable = false)
    private Boolean isTaxable;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "tax_name", length = 100)
    private String taxName;

    @Column(name = "tax_percentage", precision = 7, scale = 4)
    private BigDecimal taxPercentage;

    @Column(name = "tds_tax_id", length = 50)
    private String tdsTaxId;

    @Column(name = "is_tds_registered", nullable = false)
    private Boolean isTdsRegistered;

    @Column(name = "tax_exemption_id", length = 50)
    private String taxExemptionId;

    @Column(name = "tax_exemption_code", length = 50)
    private String taxExemptionCode;

    @Column(name = "gst_no", length = 20)
    private String gstNo;

    @Column(name = "gst_treatment", length = 50)
    private String gstTreatment;

    @Column(name = "place_of_contact", length = 10)
    private String placeOfContact;

    @Column(name = "pricebook_id")
    private Long pricebookId;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "currency_symbol", length = 10)
    private String currencySymbol;

    @Column(name = "payment_terms")
    private Integer paymentTerms;

    @Column(name = "payment_terms_label", length = 50)
    private String paymentTermsLabel;

    @Column(name = "outstanding_receivable_amount", precision = 19, scale = 4)
    private BigDecimal outstandingReceivableAmount;

    @Column(name = "outstanding_receivable_amount_bcy", precision = 19, scale = 4)
    private BigDecimal outstandingReceivableAmountBcy;

    @Column(name = "unused_credits_receivable_amount", precision = 19, scale = 4)
    private BigDecimal unusedCreditsReceivableAmount;

    @Column(name = "unused_credits_receivable_amount_bcy", precision = 19, scale = 4)
    private BigDecimal unusedCreditsReceivableAmountBcy;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "primary_contact_person_id")
    private Long primaryContactPersonId;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "facebook", length = 255)
    private String facebook;

    @Column(name = "twitter", length = 255)
    private String twitter;

    @Column(name = "is_linked_with_crm", nullable = false)
    private Boolean isLinkedWithCrm;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
