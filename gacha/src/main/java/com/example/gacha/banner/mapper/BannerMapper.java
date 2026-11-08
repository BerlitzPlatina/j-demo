package com.example.gacha.banner.mapper;

import java.time.LocalDateTime;

import com.example.gacha.banner.dto.BannerCreateRequest;
import com.example.gacha.banner.dto.BannerPatchRequest;
import com.example.gacha.banner.dto.BannerResponse;
import com.example.gacha.banner.dto.BannerUpdateRequest;
import com.example.gacha.banner.entity.Banner;
import com.example.gacha.banner.entity.BannerStatus;

/**
 * Entity to response, and request to entity. Kept in one place so no controller ever
 * serializes an entity and no service hand-copies fields.
 */
public final class BannerMapper {

    private BannerMapper() {
    }

    public static BannerResponse toResponse(Banner banner) {
        return toResponse(banner, LocalDateTime.now());
    }

    /** @param now the moment {@code live} is evaluated at, so a whole page shares one clock */
    public static BannerResponse toResponse(Banner banner, LocalDateTime now) {
        return new BannerResponse(
                banner.getId(),
                banner.getCode(),
                banner.getName(),
                banner.getDescription(),
                banner.getBannerType(),
                banner.getStatus(),
                banner.isLiveAt(now),
                banner.getImageUrl(),
                banner.getStartTime(),
                banner.getEndTime(),
                banner.getCostPerPull(),
                banner.getCurrency(),
                banner.getHardPity(),
                banner.getSoftPityStart(),
                banner.getGuaranteedFeatured(),
                banner.getDisplayOrder(),
                banner.getCreatedAt(),
                banner.getUpdatedAt());
    }

    public static Banner toEntity(BannerCreateRequest request) {
        return Banner.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .bannerType(request.bannerType())
                .status(BannerStatus.DRAFT)
                .imageUrl(request.imageUrl())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .costPerPull(request.costPerPull())
                .currency(request.currency())
                .hardPity(request.hardPity())
                .softPityStart(request.softPityStart())
                .guaranteedFeatured(orFalse(request.guaranteedFeatured()))
                .displayOrder(orZero(request.displayOrder()))
                .build();
    }

    /** Full replacement: an omitted optional field is written as null or its default. */
    public static void replace(Banner target, BannerUpdateRequest request) {
        target.setName(request.name());
        target.setDescription(request.description());
        target.setBannerType(request.bannerType());
        target.setImageUrl(request.imageUrl());
        target.setStartTime(request.startTime());
        target.setEndTime(request.endTime());
        target.setCostPerPull(request.costPerPull());
        target.setCurrency(request.currency());
        target.setHardPity(request.hardPity());
        target.setSoftPityStart(request.softPityStart());
        target.setGuaranteedFeatured(orFalse(request.guaranteedFeatured()));
        target.setDisplayOrder(orZero(request.displayOrder()));
    }

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge(Banner target, BannerPatchRequest request) {
        if (request.name() != null) {
            target.setName(request.name());
        }
        if (request.description() != null) {
            target.setDescription(request.description());
        }
        if (request.bannerType() != null) {
            target.setBannerType(request.bannerType());
        }
        if (request.imageUrl() != null) {
            target.setImageUrl(request.imageUrl());
        }
        if (request.startTime() != null) {
            target.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            target.setEndTime(request.endTime());
        }
        if (request.costPerPull() != null) {
            target.setCostPerPull(request.costPerPull());
        }
        if (request.currency() != null) {
            target.setCurrency(request.currency());
        }
        if (request.hardPity() != null) {
            target.setHardPity(request.hardPity());
        }
        if (request.softPityStart() != null) {
            target.setSoftPityStart(request.softPityStart());
        }
        if (request.guaranteedFeatured() != null) {
            target.setGuaranteedFeatured(request.guaranteedFeatured());
        }
        if (request.displayOrder() != null) {
            target.setDisplayOrder(request.displayOrder());
        }
    }

    private static Boolean orFalse(Boolean value) {
        return value != null ? value : Boolean.FALSE;
    }

    private static Integer orZero(Integer value) {
        return value != null ? value : 0;
    }
}
