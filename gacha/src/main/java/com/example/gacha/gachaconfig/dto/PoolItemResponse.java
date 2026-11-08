package com.example.gacha.gachaconfig.dto;

import java.math.BigDecimal;

import com.example.gacha.item.entity.ItemType;

/**
 * One pool entry with its item, plus {@code probability}: the base chance of exactly this item
 * per pull, in percent, before pity - what a "drop rate details" screen shows.
 */
public record PoolItemResponse(
        Long itemId,
        String itemCode,
        String itemName,
        ItemType itemType,
        Integer rarity,
        Boolean itemActive,
        Boolean featured,
        Integer weight,
        BigDecimal probability) {
}
