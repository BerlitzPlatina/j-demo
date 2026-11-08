package com.example.gacha.item.dto;

import java.util.Date;

import com.example.gacha.item.entity.ItemType;

public record ItemResponse(
        Long id,
        String code,
        String name,
        String description,
        ItemType itemType,
        Integer rarity,
        String imageUrl,
        Boolean active,
        Date createdAt,
        Date updatedAt) {
}
