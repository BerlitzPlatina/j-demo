package com.example.erp.organization.entity;

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

import java.time.LocalDate;

/**
 * A tenant of the ERP: the company the rest of the data hangs off.
 * <p>
 * Mapped against the {@code organizations} table created by the liquibase module. The id and the
 * two audit timestamps come from {@link AbstractAuditModel}, so they are not repeated here.
 */
@Entity
@Table(name = "organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Organization extends AbstractAuditModel {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_logo_uploaded")
    private Boolean logoUploaded;

    /**
     * Exactly one organization carries this flag; the service moves it rather than letting two
     * rows hold it at once.
     */
    @Column(name = "is_default_org", nullable = false)
    private Boolean defaultOrg;

    @Column(name = "user_role")
    private String userRole;

    @Column(name = "account_created_date")
    private LocalDate accountCreatedDate;

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "date_format")
    private String dateFormat;

    @Column(name = "field_separator")
    private String fieldSeparator;

    /** 1-12; a fiscal year that does not start in January is normal in ERP setups. */
    @Column(name = "fiscal_year_start_month")
    private Integer fiscalYearStartMonth;

    @Column(name = "tax_group_enabled", nullable = false)
    private Boolean taxGroupEnabled;

    @Column(name = "user_status")
    private String userStatus;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "industry_type")
    private String industryType;
}
