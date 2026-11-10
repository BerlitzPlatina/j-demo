package com.example.gacha.gachaengine.entity;

import java.time.LocalDateTime;

import com.example.common.jpa.entity.AbstractAuditModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Pull history: records each gacha pull result.
 */
@Entity
@Table(name = "pull_histories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PullHistory extends AbstractAuditModel {

    /** Business key: unique code for this pull (can be UUID) */
    @Column(name = "code", length = 50, nullable = false, unique = true, updatable = false)
    private String code;

    /** User who made this pull */
    @Column(name = "user_code", length = 50, nullable = false)
    private String userCode;

    /** User profile ID at time of pull */
    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    /** Banner ID that was pulled on */
    @Column(name = "banner_id", nullable = false)
    private Long bannerId;

    @Column(name = "banner_code", length = 50, nullable = false)
    private String bannerCode;

    /** Item ID received */
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Item code (business key) */
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    /** Rarity of item received */
    @Column(name = "rarity", nullable = false)
    private Integer rarity;

    /** Whether this was a featured/guaranteed item pull */
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured;

    /** Pull type: single or multi */
    @Enumerated(EnumType.STRING)
    @Column(name = "pull_type", length = 20, nullable = false)
    private PullType pullType;

    /** Pity counter value at time of this pull */
    @Column(name = "pity_at_pull", nullable = false)
    private Integer pityAtPull;

    /** Guaranteed counter value at time of this pull */
    @Column(name = "guaranteed_at_pull", nullable = false)
    private Integer guaranteedAtPull;

    /** Soft pity counter value at time of this pull */
    @Column(name = "soft_pity_at_pull", nullable = false)
    private Integer softPityAtPull;

    /** Whether user was guaranteed at time of pull */
    @Column(name = "was_guaranteed", nullable = false)
    private Boolean wasGuaranteed;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
