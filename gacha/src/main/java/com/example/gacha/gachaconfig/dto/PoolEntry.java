package com.example.gacha.gachaconfig.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One item in a banner pool. Omitted {@code featured} means false, omitted {@code weight} 1. */
public record PoolEntry(
        @NotNull Long itemId,
        Boolean featured,
        @Positive @Max(1_000_000) Integer weight) {
}
