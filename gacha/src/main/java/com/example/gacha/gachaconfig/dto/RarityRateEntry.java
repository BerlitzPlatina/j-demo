package com.example.gacha.gachaconfig.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * One row of a banner's rate table. Percentages, up to 4 decimals. An omitted
 * {@code featuredRate} means 0: this rarity drops only non-featured items.
 */
public record RarityRateEntry(
        @NotNull @Min(1) @Max(6) Integer rarity,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal rate,
        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal featuredRate) {
}
