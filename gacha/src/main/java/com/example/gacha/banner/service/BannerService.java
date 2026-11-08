package com.example.gacha.banner.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.web.dto.PageResponse;
import com.example.gacha.banner.dto.BannerCreateRequest;
import com.example.gacha.banner.dto.BannerPatchRequest;
import com.example.gacha.banner.dto.BannerResponse;
import com.example.gacha.banner.dto.BannerSearchRequest;
import com.example.gacha.banner.dto.BannerUpdateRequest;
import com.example.gacha.banner.entity.Banner;
import com.example.gacha.banner.entity.BannerStatus;
import com.example.gacha.banner.entity.BannerType;
import com.example.gacha.banner.mapper.BannerMapper;
import com.example.gacha.banner.repository.BannerDao;
import com.example.gacha.banner.repository.BannerSpecifications;
import com.example.gacha.common.page.PageableSupport;
import com.example.gacha.common.support.Entities;
import com.example.gacha.common.support.Guards;
import com.example.gacha.gachaconfig.service.GachaConfigService;

/**
 * Banner management: CRUD plus the DRAFT -> PUBLISHED -> ARCHIVED lifecycle.
 * <p>
 * Rules enforced here, each answered with a 400 whose message starts with the field to blame:
 * <ul>
 * <li>{@code code} is unique and immutable.</li>
 * <li>{@code endTime} is after {@code startTime}; {@code softPityStart} needs a
 * {@code hardPity} and sits below it.</li>
 * <li>An archived banner is read-only.</li>
 * <li>Once a published banner has started, its pull rules (type, start time, price, pity,
 * guarantee) are locked, since players have already pulled under them. Cosmetic fields,
 * the display order and the end time stay editable.</li>
 * <li>Only a draft can be deleted; a banner that was ever published is archived instead.</li>
 * <li>Publishing requires a complete gacha configuration (see {@link GachaConfigService}).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class BannerService {

    private static final String ENTITY = "Banner";

    private static final Set<String> SORTABLE_FIELDS = PageableSupport.sortableFields(
            "code", "name", "bannerType", "status", "startTime", "endTime", "displayOrder", "costPerPull");

    /** Player-facing order: operator's ordering first, then the newest banner. */
    private static final Sort LIVE_ORDER = Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("startTime"),
            Sort.Order.desc("id"));

    private final BannerDao bannerDao;
    private final GachaConfigService gachaConfigService;

    public BannerService(BannerDao bannerDao, GachaConfigService gachaConfigService) {
        this.bannerDao = bannerDao;
        this.gachaConfigService = gachaConfigService;
    }

    // ------------------------------------------------------------------ read

    public PageResponse<BannerResponse> search(BannerSearchRequest request, Pageable pageable) {
        Page<Banner> page = bannerDao.findAll(BannerSpecifications.from(request),
                PageableSupport.sanitize(pageable, SORTABLE_FIELDS));
        LocalDateTime now = LocalDateTime.now();
        return PageResponse.from(page, banner -> BannerMapper.toResponse(banner, now));
    }

    /** The banners a player sees right now, in display order. */
    public List<BannerResponse> getLive() {
        LocalDateTime now = LocalDateTime.now();
        return bannerDao.findAll(BannerSpecifications.liveAt(now), LIVE_ORDER).stream()
                .map(banner -> BannerMapper.toResponse(banner, now))
                .toList();
    }

    public BannerResponse getById(Long id) {
        return BannerMapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public BannerResponse create(BannerCreateRequest request) {
        Guards.assertNotTaken(bannerDao.existsByCodeIgnoreCase(request.code()), "code", "a banner", request.code());
        Banner banner = BannerMapper.toEntity(request);
        assertConsistent(banner);
        return BannerMapper.toResponse(bannerDao.save(banner));
    }

    /** Full replacement. Fields the caller left out are written as null or their default. */
    @Transactional
    public BannerResponse update(Long id, BannerUpdateRequest request) {
        Banner banner = findEditable(id);
        PullRules before = PullRules.of(banner);
        BannerMapper.replace(banner, request);
        assertEditAllowed(banner, before);
        return BannerMapper.toResponse(banner);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public BannerResponse patch(Long id, BannerPatchRequest request) {
        Banner banner = findEditable(id);
        PullRules before = PullRules.of(banner);
        BannerMapper.merge(banner, request);
        assertEditAllowed(banner, before);
        return BannerMapper.toResponse(banner);
    }

    /** DRAFT -> PUBLISHED. A banner whose window has already closed cannot be published. */
    @Transactional
    public BannerResponse publish(Long id) {
        Banner banner = findOrThrow(id);
        if (banner.getStatus() != BannerStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "status: only a DRAFT banner can be published, this one is " + banner.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        if (banner.getEndTime() != null && !banner.getEndTime().isAfter(now)) {
            throw new IllegalArgumentException("endTime: the banner window has already ended");
        }
        gachaConfigService.assertReady(id);
        banner.setStatus(BannerStatus.PUBLISHED);
        return BannerMapper.toResponse(banner, now);
    }

    /** DRAFT or PUBLISHED -> ARCHIVED. Takes the banner off the player-facing list for good. */
    @Transactional
    public BannerResponse archive(Long id) {
        Banner banner = findOrThrow(id);
        if (banner.getStatus() == BannerStatus.ARCHIVED) {
            throw new IllegalArgumentException("status: the banner is already ARCHIVED");
        }
        banner.setStatus(BannerStatus.ARCHIVED);
        return BannerMapper.toResponse(banner);
    }

    @Transactional
    public void delete(Long id) {
        Banner banner = findOrThrow(id);
        if (banner.getStatus() != BannerStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "status: only a DRAFT banner can be deleted, archive this one instead");
        }
        bannerDao.delete(banner);
    }

    // ---------------------------------------------------------------- helper

    private Banner findOrThrow(Long id) {
        return Entities.findOrThrow(bannerDao, id, ENTITY);
    }

    private Banner findEditable(Long id) {
        Banner banner = findOrThrow(id);
        if (banner.getStatus() == BannerStatus.ARCHIVED) {
            throw new IllegalArgumentException("status: an ARCHIVED banner cannot be edited");
        }
        return banner;
    }

    /**
     * Runs after the request was applied to the managed entity. Throwing rolls the transaction
     * back, so the half-applied change is never flushed.
     */
    private void assertEditAllowed(Banner banner, PullRules before) {
        assertConsistent(banner);
        boolean started = banner.getStatus() == BannerStatus.PUBLISHED
                && !before.startTime().isAfter(LocalDateTime.now());
        if (started && !before.equals(PullRules.of(banner))) {
            throw new IllegalArgumentException("status: the banner is live, its type, startTime, costPerPull, "
                    + "currency, hardPity, softPityStart and guaranteedFeatured can no longer change");
        }
    }

    private static void assertConsistent(Banner banner) {
        if (banner.getEndTime() != null && !banner.getEndTime().isAfter(banner.getStartTime())) {
            throw new IllegalArgumentException("endTime: must be after startTime");
        }
        if (banner.getSoftPityStart() != null) {
            if (banner.getHardPity() == null) {
                throw new IllegalArgumentException("softPityStart: requires hardPity");
            }
            if (banner.getSoftPityStart() >= banner.getHardPity()) {
                throw new IllegalArgumentException("softPityStart: must be less than hardPity");
            }
        }
    }

    /** The fields players have pulled under; a record so a snapshot compares by value. */
    private record PullRules(BannerType bannerType, LocalDateTime startTime, Integer costPerPull, String currency,
            Integer hardPity, Integer softPityStart, Boolean guaranteedFeatured) {

        static PullRules of(Banner banner) {
            return new PullRules(banner.getBannerType(), banner.getStartTime(), banner.getCostPerPull(),
                    banner.getCurrency(), banner.getHardPity(), banner.getSoftPityStart(),
                    Objects.requireNonNullElse(banner.getGuaranteedFeatured(), Boolean.FALSE));
        }
    }
}
