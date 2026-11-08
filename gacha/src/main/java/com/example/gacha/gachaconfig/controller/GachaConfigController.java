package com.example.gacha.gachaconfig.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.common.web.dto.ApiResponse;
import com.example.gacha.common.constant.ApiPaths;
import com.example.gacha.gachaconfig.dto.GachaConfigResponse;
import com.example.gacha.gachaconfig.dto.PoolRequest;
import com.example.gacha.gachaconfig.dto.RarityRatesRequest;
import com.example.gacha.gachaconfig.service.GachaConfigService;

import jakarta.validation.Valid;

/** Gacha configuration of one banner: drop rates and item pool. */
@RestController
@RequestMapping(ApiPaths.BANNER_CONFIG)
public class GachaConfigController {

    private final GachaConfigService gachaConfigService;

    public GachaConfigController(GachaConfigService gachaConfigService) {
        this.gachaConfigService = gachaConfigService;
    }

    /** GET /api/banners/{bannerId}/config - rates, pool with per-item probability, readiness. */
    @GetMapping
    public ResponseEntity<ApiResponse<GachaConfigResponse>> getConfig(@PathVariable Long bannerId) {
        return ResponseEntity.ok(ApiResponse.success(gachaConfigService.getConfig(bannerId)));
    }

    /** PUT /api/banners/{bannerId}/config/rates - replaces the whole rate table. */
    @PutMapping("/rates")
    public ResponseEntity<ApiResponse<GachaConfigResponse>> replaceRates(
            @PathVariable Long bannerId, @Valid @RequestBody RarityRatesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gachaConfigService.replaceRates(bannerId, request)));
    }

    /** PUT /api/banners/{bannerId}/config/pool - replaces the whole item pool. */
    @PutMapping("/pool")
    public ResponseEntity<ApiResponse<GachaConfigResponse>> replacePool(
            @PathVariable Long bannerId, @Valid @RequestBody PoolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gachaConfigService.replacePool(bannerId, request)));
    }
}
