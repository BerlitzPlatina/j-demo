package com.example.erp.position.entity;

import org.hibernate.annotations.BatchSize;
// import org.hibernate.annotations.Fetch;
// import org.hibernate.annotations.FetchMode;

import com.example.common.jpa.entity.AbstractAuditModel;
import com.example.erp.job.entity.Job;
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
 * The {@code positions} table.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}, so
 * they are not
 * repeated here.
 */
@Entity
@Table(name = "positions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Position extends AbstractAuditModel {

    /**
     * Owning side of the relation. Excluded from {@code toString}/{@code equals}
     * so printing
     * this row does not walk into LegalEntity and recurse.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    // @ToString.Exclude
    // @EqualsAndHashCode.Exclude
    private LegalEntity legalEntity;

    // /**
    // * Owning side of the relation. Excluded from {@code toString}/{@code equals}
    // * so printing
    // * this row does not walk into Job and recurse.
    // */
    // @ManyToOne(fetch = FetchType.LAZY, optional = true)
    // @JoinColumn(name = "job_id")
    // @ToString.Exclude
    // @EqualsAndHashCode.Exclude
    // private Job job;

    @Column(name = "legacy_position_id", nullable = false, unique = true)
    private Integer legacyPositionId;

    @Column(name = "position_code", nullable = false)
    private String positionCode;

    @Column(name = "legacy_position_code")
    private String legacyPositionCode;

    @Column(name = "position_name", nullable = false, columnDefinition = "TEXT")
    private String positionName;

    @Column(name = "position_status", nullable = false)
    private String positionStatus;

    @Column(name = "reference_quality", nullable = false)
    private String referenceQuality;

    @Column(name = "source", nullable = false)
    private String source;
}
