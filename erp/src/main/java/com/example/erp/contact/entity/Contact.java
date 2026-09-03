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
import java.time.LocalDate;

// wtf
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
    private Long organization_id;

    @Column(length = 50)
    private String contact_number;

    @Column(length = 255, nullable = false)
    private String contact_name;

    @Column(length = 255)
    private String company_name;

    @Column(length = 255)
    private String legal_name;

    @Column(length = 20, nullable = false)
    private String contact_type;

    @Column(length = 20)
    private String customer_sub_type;

    @Column(length = 20)
    private String status;

    @Column(nullable = false)
    private Boolean has_transaction;

    @Column(precision = 19, scale = 4)
    private BigDecimal credit_limit;

    @Column(nullable = false)
    private Boolean payment_reminder_enabled;

    @Column(length = 10)
    private String language_code;

    @Column(nullable = false)
    private Boolean is_taxable;

    @Column(length = 50)
    private String tax_id;

    @Column(length = 100)
    private String tax_name;

    @Column(precision = 7, scale = 4)
    private BigDecimal tax_percentage;

    @Column(length = 50)
    private String tds_tax_id;

    @Column(nullable = false)
    private Boolean is_tds_registered;

    @Column(length = 50)
    private String tax_exemption_id;

    @Column(length = 50)
    private String tax_exemption_code;

    @Column(length = 20)
    private String gst_no;

    @Column(length = 50)
    private String gst_treatment;

    @Column(length = 10)
    private String place_of_contact;

    private Long pricebook_id;
    private Long currency_id;

    @Column(length = 3)
    private String currency_code;

    @Column(length = 10)
    private String currency_symbol;

    private Integer payment_terms;

    @Column(length = 50)
    private String payment_terms_label;

    @Column(precision = 19, scale = 4)
    private BigDecimal outstanding_receivable_amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal outstanding_receivable_amount_bcy;

    @Column(precision = 19, scale = 4)
    private BigDecimal unused_credits_receivable_amount;

    @Column(precision = 19, scale = 4)
    private BigDecimal unused_credits_receivable_amount_bcy;

    private Long owner_id;
    private Long primary_contact_person_id;

    @Column(length = 255)
    private String website;

    @Column(length = 255)
    private String facebook;

    @Column(length = 255)
    private String twitter;

    @Column(nullable = false)
    private Boolean is_linked_with_crm;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
