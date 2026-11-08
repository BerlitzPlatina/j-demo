package com.example.gacha.banner.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.example.gacha.banner.dto.BannerSearchRequest;
import com.example.gacha.banner.entity.Banner;
import com.example.gacha.banner.entity.BannerStatus;
import com.example.gacha.common.constant.Constants;
import com.example.gacha.common.specification.SpecBuilder;

/**
 * The {@code where} of the banner search. {@link SpecBuilder} drops every filter the request
 * left out; {@link #liveAt} is the one condition it does not model.
 */
public final class BannerSpecifications {

    private BannerSpecifications() {
    }

    public static Specification<Banner> from(BannerSearchRequest request) {
        return SpecBuilder.<Banner>of()
                .likeAny(request.keyword(), "code", "name", "description")
                .eq("bannerType", request.bannerType())
                .eq("status", request.status())
                .and(liveAt(request.liveAt()))
                .between("startTime", request.startFrom(), request.startTo())
                .between("endTime", request.endFrom(), request.endTo())
                .dayRange(Constants.CREATE_TIME, request.createdFrom(), request.createdTo())
                .build();
    }

    /**
     * {@code status = PUBLISHED and start_time <= at and (end_time is null or end_time > at)} -
     * the same rule as {@link Banner#isLiveAt}, so the list and the {@code live} flag agree.
     *
     * @return null when no moment was given, which the builder reads as "no filter"
     */
    public static Specification<Banner> liveAt(LocalDateTime at) {
        if (at == null) {
            return null;
        }
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), BannerStatus.PUBLISHED),
                cb.lessThanOrEqualTo(root.get("startTime"), at),
                cb.or(cb.isNull(root.get("endTime")), cb.greaterThan(root.get("endTime"), at)));
    }
}
