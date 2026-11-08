package com.example.gacha.banner.dto;

import java.time.LocalDateTime;

import com.example.gacha.banner.entity.BannerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/banners}.
 * <p>
 * There is no status: a new banner always starts as {@code DRAFT} and goes live through
 * {@code POST /api/banners/{id}/publish}. Omitted {@code guaranteedFeatured} falls back to
 * false and omitted {@code displayOrder} to 0.
 */
public record BannerCreateRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Z0-9_-]+$",
                message = "must contain only upper-case letters, digits, '_' or '-'") String code,
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull BannerType bannerType,
        @Size(max = 512) String imageUrl,
        @NotNull LocalDateTime startTime,
        LocalDateTime endTime,
        @NotNull @Positive Integer costPerPull,
        @NotBlank @Size(max = 32) String currency,
        @Positive Integer hardPity,
        @Positive Integer softPityStart,
        Boolean guaranteedFeatured,
        @PositiveOrZero Integer displayOrder) {
}
