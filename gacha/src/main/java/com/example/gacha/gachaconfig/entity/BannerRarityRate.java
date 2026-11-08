package com.example.gacha.gachaconfig.entity;

import java.math.BigDecimal;

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
 * The {@code banner_rarity_rates} table: one row per (banner, rarity).
 * <p>
 * A pull first picks a rarity by {@link #rate}; then, within that rarity, it picks a featured
 * item with probability {@link #featuredRate} and a non-featured one otherwise; finally it picks
 * an item of that group by weight. Rates of one banner add up to exactly 100.
 */
@Entity
@Table(name = "banner_rarity_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BannerRarityRate extends AbstractAuditModel {

    @Column(name = "banner_id", nullable = false)
    private Long bannerId;

    @Column(name = "rarity", nullable = false)
    private Integer rarity;

    /** Base chance of this rarity per pull, in percent (0..100), before pity. */
    @Column(name = "rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal rate;

    /** Share of this rarity that goes to featured items, in percent; 50 is a 50/50. */
    @Column(name = "featured_rate", precision = 7, scale = 4, nullable = false)
    private BigDecimal featuredRate;
}
