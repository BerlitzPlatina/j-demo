package com.example.gacha.item.dto;

import com.example.gacha.item.entity.ItemType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Payload for {@code PATCH /api/items/{id}}: a null field means "leave it as it is". */
public record ItemPatchRequest(
        @Size(min = 1, max = 255) String name,
        String description,
        ItemType itemType,
        @Min(1) @Max(6) Integer rarity,
        @Size(max = 512) String imageUrl,
        Boolean active) {
}
