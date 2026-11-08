package com.example.gacha.gachaconfig.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/banners/{id}/config/rates}: the whole rate table, replacing the
 * current one. Rarities are distinct and the rates add up to exactly 100; an empty list clears
 * the table.
 */
public record RarityRatesRequest(
        @NotNull @Size(max = 6) List<@NotNull @Valid RarityRateEntry> rates) {
}
