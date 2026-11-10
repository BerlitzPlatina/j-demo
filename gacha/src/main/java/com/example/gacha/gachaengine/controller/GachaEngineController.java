package com.example.gacha.gachaengine.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.gacha.gachaengine.dto.MultiPullResultResponse;
import com.example.gacha.gachaengine.dto.PullRequest;
import com.example.gacha.gachaengine.dto.PullStatisticsResponse;
import com.example.gacha.gachaengine.dto.UserProfileResponse;
import com.example.gacha.gachaengine.service.GachaEngine;
import com.example.gacha.gachaengine.service.UserProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST API for gacha pulls.
 * 
 * API Endpoints:
 * - POST /api/gacha/pull - Perform a gacha pull (single or multi)
 * - GET /api/gacha/profile/{userCode}/{bannerCode} - Get user profile
 * - GET /api/gacha/statistics/{userCode}/{bannerId} - Get pull statistics
 * - PUT /api/gacha/profile/{userCode}/{bannerCode}/guarantee - Set guaranteed status
 * - PUT /api/gacha/profile/{userCode}/{bannerCode}/reset - Reset pity counters
 */
@Slf4j
@RestController
@RequestMapping("/api/gacha")
public class GachaEngineController {

    private final GachaEngine gachaEngine;
    private final UserProfileService profileService;

    public GachaEngineController(GachaEngine gachaEngine, UserProfileService profileService) {
        this.gachaEngine = gachaEngine;
        this.profileService = profileService;
    }

    /**
     * Perform a gacha pull (single or multi).
     * 
     * @param request PullRequest containing userCode, bannerCode, and quantity
     * @return MultiPullResultResponse with pull results and statistics
     */
    @PostMapping("/pull")
    public ResponseEntity<MultiPullResultResponse> performPull(@RequestBody PullRequest request) {
        log.info("Performing gacha pull for user: {}, banner: {}, quantity: {}", 
                request.getUserCode(), request.getBannerCode(), request.getQuantity());

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            request.setQuantity(1);
        }

        MultiPullResultResponse result = gachaEngine.pull(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Get user profile for a banner.
     */
    @GetMapping("/profile/{userCode}/{bannerCode}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String userCode,
            @PathVariable String bannerCode) {
        log.info("Getting profile for user: {}, banner: {}", userCode, bannerCode);

        UserProfileResponse response = profileService.getProfile(userCode, bannerCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Get pull statistics for a user on a banner.
     */
    @GetMapping("/statistics/{userCode}/{bannerId}")
    public ResponseEntity<PullStatisticsResponse> getStatistics(@PathVariable String userCode,
            @PathVariable Long bannerId) {
        log.info("Getting statistics for user: {}, banner ID: {}", userCode, bannerId);

        PullStatisticsResponse response = profileService.getStatistics(userCode, bannerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Set guaranteed status.
     */
    @PutMapping("/profile/{userCode}/{bannerCode}/guarantee")
    public ResponseEntity<UserProfileResponse> setGuaranteed(@PathVariable String userCode,
            @PathVariable String bannerCode, @RequestParam Boolean guaranteed) {
        log.info("Setting guaranteed={} for user: {}, banner: {}", guaranteed, userCode, bannerCode);

        UserProfileResponse response = profileService.setGuaranteed(userCode, bannerCode, guaranteed);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset pity counters.
     */
    @PutMapping("/profile/{userCode}/{bannerCode}/reset")
    public ResponseEntity<UserProfileResponse> resetPity(@PathVariable String userCode,
            @PathVariable String bannerCode) {
        log.info("Resetting pity for user: {}, banner: {}", userCode, bannerCode);

        UserProfileResponse response = profileService.resetPity(userCode, bannerCode);
        return ResponseEntity.ok(response);
    }
}
