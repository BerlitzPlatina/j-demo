package com.example.erp.tax.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * The {@code taxes} table.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}, so
 * they are not
 * repeated here.
 */
@Entity
@Table(name = "taxes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Tax extends AbstractAuditModel {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "percentage", precision = 10, scale = 4)
    private BigDecimal percentage;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "factor", length = 50, nullable = false)
    private String factor;

    @Column(name = "specific_type", length = 50)
    private String specificType;

    @Column(name = "authority_name", length = 255)
    private String authorityName;

    @Column(name = "authority_id", length = 100)
    private String authorityId;

    @Column(name = "is_value_added", nullable = false)
    private Boolean isValueAdded;

    @Column(name = "tax_account_id")
    private Long taxAccountId;

    @Column(name = "purchase_tax_account_id")
    private Long purchaseTaxAccountId;

    @Column(name = "purchase_tax_expense_account_id")
    private Long purchaseTaxExpenseAccountId;

    @Column(name = "tds_payable_account_id")
    private Long tdsPayableAccountId;

    @Column(name = "tcs_receivable_account_id")
    private Long tcsReceivableAccountId;

    @Column(name = "tcs_payable_account_id")
    private Long tcsPayableAccountId;

    @Column(name = "account_tracking", length = 50)
    private String accountTracking;

    @Column(name = "is_non_advol_tax", nullable = false)
    private Boolean isNonAdvolTax;

    @Column(name = "is_state_cess", nullable = false)
    private Boolean isStateCess;

    @Column(name = "diff_rate_reason", length = 100)
    private String diffRateReason;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description")
    private String description;
}
