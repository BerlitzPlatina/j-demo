package com.example.gacha.banner.entity;

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
 * The {@code banners} table: one gacha banner, its time window and its pull rules.
 * <p>
 * The id and the two audit timestamps come from {@link AbstractAuditModel}.
 */
@Entity
@Table(name = "banners")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Banner extends AbstractAuditModel {

    /** Business key, immutable after creation; clients and seeds refer to a banner by it. */
    @Column(name = "code", length = 50, nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "banner_type", length = 20, nullable = false)
    private BannerType bannerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BannerStatus status;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /** Null means open-ended, which is how a standard banner stays up permanently. */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /** Price of a single pull, in {@link #currency}. A ten-pull is priced by the pull service. */
    @Column(name = "cost_per_pull", nullable = false)
    private Integer costPerPull;

    @Column(name = "currency", length = 32, nullable = false)
    private String currency;

    /** Pull count at which the top rarity is guaranteed; null means no pity. */
    @Column(name = "hard_pity")
    private Integer hardPity;

    /** Pull count from which the top-rarity rate starts ramping up; must be below hard pity. */
    @Column(name = "soft_pity_start")
    private Integer softPityStart;

    /** Whether losing a 50/50 guarantees the featured unit on the next top-rarity pull. */
    @Column(name = "guaranteed_featured", nullable = false)
    private Boolean guaranteedFeatured;

    /** Lower comes first in the player-facing list. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Published and already opened (possibly also closed since): players may have pulled on it,
     * so its pull rules and gacha configuration are frozen.
     */
    public boolean hasStartedAt(LocalDateTime at) {
        return status == BannerStatus.PUBLISHED && !at.isBefore(startTime);
    }

    /** Published, and the given moment falls inside {@code [startTime, endTime)}. */
    public boolean isLiveAt(LocalDateTime at) {
        return status == BannerStatus.PUBLISHED
                && !at.isBefore(startTime)
                && (endTime == null || at.isBefore(endTime));
    }
}
