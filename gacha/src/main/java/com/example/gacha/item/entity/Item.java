package com.example.gacha.item.entity;

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
 * The {@code items} table: the catalogue every banner pool draws from.
 * <p>
 * The rarity lives on the item, not on the pool entry, so an item is the same star level on
 * every banner and the rate table of a banner can be checked against it.
 */
@Entity
@Table(name = "items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Item extends AbstractAuditModel {

    public static final int MIN_RARITY = 1;
    public static final int MAX_RARITY = 6;

    /** Business key, immutable after creation. */
    @Column(name = "code", length = 50, nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20, nullable = false)
    private ItemType itemType;

    /** Star level, {@value #MIN_RARITY}..{@value #MAX_RARITY}. */
    @Column(name = "rarity", nullable = false)
    private Integer rarity;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** An inactive item cannot be added to a pool; it stays in the catalogue for history. */
    @Column(name = "active", nullable = false)
    private Boolean active;
}
