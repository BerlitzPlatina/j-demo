package com.example.gacha.banner.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.gacha.banner.entity.BannerStatus;
import com.example.gacha.banner.entity.BannerType;

/**
 * Query parameters of {@code GET /api/banners}. Every field is optional: one left out is one
 * filter fewer in the SQL. The {@code ...From}/{@code ...To} pairs are inclusive ranges.
 */
public record BannerSearchRequest(
        /** One search box, matched against the code, name and description. */
        String keyword,
        BannerType bannerType,
        BannerStatus status,
        /** Only banners that are published and inside their window at this moment. */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime liveAt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTo,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
}
