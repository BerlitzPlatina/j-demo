package com.example.gacha.gachaconfig.dto;

import java.math.BigDecimal;

public record RarityRateResponse(Integer rarity, BigDecimal rate, BigDecimal featuredRate) {
}
