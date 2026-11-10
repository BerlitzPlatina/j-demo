package com.example.gacha.gachaengine.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.gacha.gachaengine.dto.PullResultResponse;
import com.example.gacha.gachaengine.service.PullHistoryService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST API for pull history.
 * 
 * API Endpoints:
 * - GET /api/gacha/history/{userCode}/{bannerId} - Get pull history
 * - GET /api/gacha/history/{userCode}/{bannerId}/latest - Get latest pulls
 * - GET /api/gacha/history/{userCode}/all - Get all user pulls
 */
@Slf4j
@RestController
@RequestMapping("/api/gacha/history")
public class PullHistoryController {

    private final PullHistoryService historyService;

    public PullHistoryController(PullHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * Get pull history for a user on a banner.
     */
    @GetMapping("/{userCode}/{bannerId}")
    public ResponseEntity<List<PullResultResponse>> getPullHistory(@PathVariable String userCode,
            @PathVariable Long bannerId) {
        log.info("Getting pull history for user: {}, banner ID: {}", userCode, bannerId);

        List<PullResultResponse> history = historyService.getPullHistory(userCode, bannerId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get latest pulls for a user on a banner.
     */
    @GetMapping("/{userCode}/{bannerId}/latest")
    public ResponseEntity<List<PullResultResponse>> getLatestPulls(@PathVariable String userCode,
            @PathVariable Long bannerId, @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting latest {} pulls for user: {}, banner ID: {}", limit, userCode, bannerId);

        List<PullResultResponse> history = historyService.getLatestPulls(userCode, bannerId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * Get all pulls for a user across all banners.
     */
    @GetMapping("/{userCode}/all")
    public ResponseEntity<List<PullResultResponse>> getUserPullHistory(@PathVariable String userCode) {
        log.info("Getting all pull history for user: {}", userCode);

        List<PullResultResponse> history = historyService.getUserPullHistory(userCode);
        return ResponseEntity.ok(history);
    }

    /**
     * Get pulls by rarity for a user.
     */
    @GetMapping("/{userCode}/rarity")
    public ResponseEntity<List<PullResultResponse>> getPullsByRarity(@PathVariable String userCode,
            @RequestParam Integer minRarity) {
        log.info("Getting pulls with rarity >= {} for user: {}", minRarity, userCode);

        List<PullResultResponse> history = historyService.getPullsByRarity(userCode, minRarity);
        return ResponseEntity.ok(history);
    }
}
