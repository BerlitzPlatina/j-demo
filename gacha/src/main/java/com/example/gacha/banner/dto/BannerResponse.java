package com.example.gacha.banner.dto;

import java.time.LocalDateTime;
import java.util.Date;

import com.example.gacha.banner.entity.BannerStatus;
import com.example.gacha.banner.entity.BannerType;

/**
 * Response for a banner. {@code live} is computed at response time from the status and the
 * time window, it is not stored.
 */
public record BannerResponse(
        Long id,
        String code,
        String name,
        String description,
        BannerType bannerType,
        BannerStatus status,
        boolean live,
        String imageUrl,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer costPerPull,
        String currency,
        Integer hardPity,
        Integer softPityStart,
        Boolean guaranteedFeatured,
        Integer displayOrder,
        Date createdAt,
        Date updatedAt) {
}
