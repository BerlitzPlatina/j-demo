package com.example.gacha.banner.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.gacha.banner.dto.BannerCreateRequest;
import com.example.gacha.banner.dto.BannerPatchRequest;
import com.example.gacha.banner.dto.BannerResponse;
import com.example.gacha.banner.dto.BannerSearchRequest;
import com.example.gacha.banner.dto.BannerUpdateRequest;
import com.example.gacha.banner.service.BannerService;
import com.example.gacha.common.constant.ApiPaths;
import com.example.gacha.common.constant.Constants;

import jakarta.validation.Valid;

/**
 * Banner management API. Every method answers with the {@link ApiResponse} envelope and
 * {@link BannerResponse} records; entities never leave the service layer.
 */
@RestController
@RequestMapping(ApiPaths.BANNERS)
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    /**
     * GET /api/banners?keyword=summer&amp;bannerType=LIMITED&amp;status=PUBLISHED
     * &amp;liveAt=2026-11-07T12:00:00&amp;page=0&amp;size=10&amp;sort=startTime,desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BannerResponse>>> getBanners(
            @ModelAttribute BannerSearchRequest request,
            @PageableDefault(size = Constants.DEFAULT_PAGE_SIZE, sort = Constants.ID,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.search(request, pageable)));
    }

    /**
     * GET /api/banners/live - what a player sees now, in display order. Mapped before
     * {@code /{id}} only for readability; Spring prefers the literal path either way.
     */
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getLiveBanners() {
        return ResponseEntity.ok(ApiResponse.success(bannerService.getLive()));
    }

    /** GET /api/banners/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> getBanner(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.getById(id)));
    }

    /** POST /api/banners - always created as DRAFT. */
    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(
            @Valid @RequestBody BannerCreateRequest request) {
        BannerResponse created = bannerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /** PUT /api/banners/{id} - full replacement. */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> replaceBanner(
            @PathVariable Long id, @Valid @RequestBody BannerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.update(id, request)));
    }

    /** PATCH /api/banners/{id} - only the fields present in the body are changed. */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> patchBanner(
            @PathVariable Long id, @Valid @RequestBody BannerPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.patch(id, request)));
    }

    /** POST /api/banners/{id}/publish - DRAFT -> PUBLISHED. */
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<BannerResponse>> publishBanner(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.publish(id)));
    }

    /** POST /api/banners/{id}/archive - DRAFT/PUBLISHED -> ARCHIVED. */
    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<BannerResponse>> archiveBanner(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.archive(id)));
    }

    /** DELETE /api/banners/{id} - drafts only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
