package com.example.gacha.gachaengine.entity;

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
 * User profile for gacha operations: tracks pulls, inventory, and pity status.
 * Each user has one profile per banner.
 */
@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserProfile extends AbstractAuditModel {

    /** Business key: external service user ID */
    @Column(name = "user_code", length = 50, nullable = false)
    private String userCode;

    /** Banner this profile is associated with */
    @Column(name = "banner_id", nullable = false)
    private Long bannerId;

    @Column(name = "banner_code", length = 50, nullable = false)
    private String bannerCode;

    /** Total number of pulls on this banner */
    @Column(name = "total_pulls", nullable = false)
    private Integer totalPulls;

    /** Pity counter: pulls since last guaranteed/featured item */
    @Column(name = "pity_counter", nullable = false)
    private Integer pityCounter;

    /** Guaranteed pity counter for soft pity (increased rates) */
    @Column(name = "guaranteed_counter", nullable = false)
    private Integer guaranteedCounter;

    /** Whether the player is guaranteed the featured item on next guaranteed pull */
    @Column(name = "is_guaranteed", nullable = false)
    private Boolean isGuaranteed;

    /** Last item ID received from guaranteed pull (to prevent duplicate guaranteed on next time) */
    @Column(name = "last_guaranteed_item_id")
    private Long lastGuaranteedItemId;

    /** Last 5-star item ID (for guaranteed rotation) */
    @Column(name = "last_five_star_item_id")
    private Long lastFiveStarItemId;

    /** Accumulated soft pity counter (for rate-up at 75+ pulls) */
    @Column(name = "soft_pity_counter", nullable = false)
    private Integer softPityCounter;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
