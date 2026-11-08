package com.example.gacha.item.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.gacha.item.entity.ItemType;

/** Query parameters of {@code GET /api/items}; every field is optional. */
public record ItemSearchRequest(
        /** Matched against the code, name and description. */
        String keyword,
        ItemType itemType,
        Integer rarity,
        Boolean active,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
}
