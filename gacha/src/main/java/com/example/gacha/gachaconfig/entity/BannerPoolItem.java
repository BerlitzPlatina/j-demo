package com.example.gacha.gachaconfig.entity;

import com.example.common.jpa.entity.AbstractAuditModel;
import com.example.gacha.item.entity.Item;

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
 * The {@code banner_pool_items} table: which items a banner can drop.
 * <p>
 * The rarity comes from the {@link Item}; this row only adds whether the item is featured on
 * this banner and its weight inside its (rarity, featured) group.
 */
@Entity
@Table(name = "banner_pool_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "item")
@ToString(callSuper = true, exclude = "item")
public class BannerPoolItem extends AbstractAuditModel {

    @Column(name = "banner_id", nullable = false)
    private Long bannerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "featured", nullable = false)
    private Boolean featured;

    /** Relative weight within the (rarity, featured) group; 1 for an even split. */
    @Column(name = "weight", nullable = false)
    private Integer weight;
}
