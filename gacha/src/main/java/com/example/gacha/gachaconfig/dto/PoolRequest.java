package com.example.gacha.gachaconfig.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/banners/{id}/config/pool}: the whole pool, replacing the current
 * one. Items are distinct, exist and are active.
 */
public record PoolRequest(
        @NotNull @Size(max = 1000) List<@NotNull @Valid PoolEntry> items) {
}
