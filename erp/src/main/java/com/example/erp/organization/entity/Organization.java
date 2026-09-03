package com.example.erp.organization.entity;

import com.example.common.jpa.entity.AbstractAuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A tenant of the ERP: the company the rest of the data hangs off.
 * <p>
 * Mapped against the {@code organizations} table created by the liquibase
 * module. The id and the
 * two audit timestamps come from {@link AbstractAuditModel}, so they are not
 * repeated here.
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
     * Exactly one organization carries this flag; the service moves it rather than
     * letting two
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

    /**
     * 1-12; a fiscal year that does not start in January is normal in ERP setups.
     */
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

    /**
     * Addresses of this organization, read-only from here: no cascade, so deleting
     * an
     * organization that still has addresses keeps failing on the foreign key
     * instead of
     * quietly taking the addresses with it.
     * <p>
     * Excluded from {@code toString}/{@code equals} because touching a lazy
     * collection there
     * would either recurse or fail outside a transaction.
     * <p>
     * {@code SUBSELECT} decides how the collection is loaded once something touches
     * it: instead
     * of one query per organization, Hibernate re-runs the query that loaded the
     * organizations
     * as a subquery, and fetches every collection in one statement:
     * 
     * <pre>
     * select ... from addresses
     * where organization_id in (select o.id from organizations o where &lt;original where&gt;)
     * </pre>
     * <p>
     * <b>Careful with pagination.</b> That subquery carries the original
     * {@code where} but not
     * its {@code limit}/{@code offset}, so a paged read loads the addresses of
     * every matching
     * organization, not just the page. {@code OrganizationAddressFetchTest}
     * measures exactly
     * that. {@code @BatchSize(size = 50)} is the alternative that stays inside the
     * page - it
     * binds the ids that were actually loaded - and an {@code @EntityGraph} on the
     * query avoids
     * the second statement altogether.
     */
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    // @BatchSize(size = 50)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Address> addresses = new ArrayList<>();
}
