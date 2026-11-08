package com.example.gacha.banner.dto;

import java.time.LocalDateTime;

import com.example.gacha.banner.entity.BannerType;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PATCH /api/banners/{id}}: every field is optional and a null one means
 * "leave this column as it is". Clearing a column (e.g. making a banner open-ended again) needs
 * PUT.
 */
public record BannerPatchRequest(
        @Size(min = 1, max = 255) String name,
        String description,
        BannerType bannerType,
        @Size(max = 512) String imageUrl,
        LocalDateTime startTime,
        LocalDateTime endTime,
        @Positive Integer costPerPull,
        @Size(min = 1, max = 32) String currency,
        @Positive Integer hardPity,
        @Positive Integer softPityStart,
        Boolean guaranteedFeatured,
        @PositiveOrZero Integer displayOrder) {
}
