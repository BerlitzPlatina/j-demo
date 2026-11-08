package com.example.gacha.banner.dto;

import java.time.LocalDateTime;

import com.example.gacha.banner.entity.BannerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/banners/{id}}: a full replacement, so an omitted optional field is
 * written as null (or its default).
 * <p>
 * Neither the code nor the status is here: the code is immutable and the status only moves
 * through the publish/archive endpoints.
 */
public record BannerUpdateRequest(
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
