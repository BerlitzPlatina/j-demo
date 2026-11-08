package com.example.gacha.item.dto;

import com.example.gacha.item.entity.ItemType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for {@code POST /api/items}. An omitted {@code active} falls back to true. */
public record ItemCreateRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z0-9_-]+$",
                message = "must contain only upper-case letters, digits, '_' or '-'") String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull ItemType itemType,
        @NotNull @Min(1) @Max(6) Integer rarity,
        @Size(max = 512) String imageUrl,
        Boolean active) {
}
