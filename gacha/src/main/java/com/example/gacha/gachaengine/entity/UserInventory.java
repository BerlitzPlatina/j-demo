package com.example.gacha.gachaengine.entity;

import com.example.common.jpa.entity.AbstractAuditModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * User inventory: tracks items owned by users.
 */
@Entity
@Table(name = "user_inventories",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"user_code", "item_id"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserInventory extends AbstractAuditModel {

    /** User who owns this item */
    @Column(name = "user_code", length = 50, nullable = false)
    private String userCode;

    /** Item ID owned */
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Item code (business key) */
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    /** Quantity owned */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** How many times this specific item was obtained */
    @Column(name = "times_obtained", nullable = false)
    private Integer timesObtained;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
