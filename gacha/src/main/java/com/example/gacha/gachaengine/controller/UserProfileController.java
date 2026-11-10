package com.example.gacha.gachaengine.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gacha.gachaengine.dto.UserProfileResponse;
import com.example.gacha.gachaengine.service.UserProfileService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST API for user profiles.
 * 
 * API Endpoints:
 * - GET /api/gacha/profiles/{userCode} - Get all user profiles
 */
@Slf4j
@RestController
@RequestMapping("/api/gacha/profiles")
public class UserProfileController {

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Get all user profiles across all banners.
     */
    @GetMapping("/{userCode}")
    public ResponseEntity<List<UserProfileResponse>> getUserProfiles(@PathVariable String userCode) {
        log.info("Getting all profiles for user: {}", userCode);

        List<UserProfileResponse> profiles = profileService.getUserProfiles(userCode);
        return ResponseEntity.ok(profiles);
    }
}
