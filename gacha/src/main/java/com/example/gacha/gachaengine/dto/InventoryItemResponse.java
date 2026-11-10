package com.example.gacha.gachaengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for inventory item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {

    private Long id;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String itemType;
    private Integer rarity;
    private Integer quantity;
    private Integer timesObtained;
    private String itemImageUrl;
}
