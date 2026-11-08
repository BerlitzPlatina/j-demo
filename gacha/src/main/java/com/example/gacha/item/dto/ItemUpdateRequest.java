package com.example.gacha.item.dto;

import com.example.gacha.item.entity.ItemType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for {@code PUT /api/items/{id}}: full replacement; the code is immutable. */
public record ItemUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull ItemType itemType,
        @NotNull @Min(1) @Max(6) Integer rarity,
        @Size(max = 512) String imageUrl,
        Boolean active) {
}
