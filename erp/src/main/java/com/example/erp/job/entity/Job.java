package com.example.erp.job.entity;

import com.example.common.jpa.entity.AbstractAuditModel;
import com.example.erp.legalentity.entity.LegalEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The {@code jobs} table.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}, so
 * they are not
 * repeated here.
 */
@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Job extends AbstractAuditModel {

    /**
     * Owning side of the relation. Excluded from {@code toString}/{@code equals} so
     * printing
     * this row does not walk into LegalEntity and recurse.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LegalEntity legalEntity;

    @Column(name = "job_code", nullable = false)
    private String jobCode;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "job_status", nullable = false)
    private String jobStatus;

    @Column(name = "reference_quality", nullable = false)
    private String referenceQuality;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "updated_by")
    private Integer updatedBy;
}
