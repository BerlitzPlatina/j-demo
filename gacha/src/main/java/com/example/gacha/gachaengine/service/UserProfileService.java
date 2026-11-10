package com.example.gacha.gachaengine.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gacha.banner.repository.BannerDao;
import com.example.gacha.gachaengine.dto.PullStatisticsResponse;
import com.example.gacha.gachaengine.dto.UserProfileResponse;
import com.example.gacha.gachaengine.entity.UserProfile;
import com.example.gacha.gachaengine.repository.UserProfileDao;
import lombok.extern.slf4j.Slf4j;

/**
 * User profile management service.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileDao profileDao;
    private final BannerDao bannerDao;
    private final GachaEngine gachaEngine;

    public UserProfileService(UserProfileDao profileDao, BannerDao bannerDao, GachaEngine gachaEngine) {
        this.profileDao = profileDao;
        this.bannerDao = bannerDao;
        this.gachaEngine = gachaEngine;
    }

    /**
     * Get user profile for a banner.
     */
    public UserProfileResponse getProfile(String userCode, String bannerCode) {
        return profileDao.findByUserCodeAndBannerCode(userCode, bannerCode)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found for user: " + userCode + ", banner: " + bannerCode));
    }

    /**
     * Get all user profiles.
     */
    public List<UserProfileResponse> getUserProfiles(String userCode) {
        return profileDao.findByUserCode(userCode).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get user profile with statistics.
     */
    public UserProfileResponse getProfileWithStats(String userCode, String bannerCode) {
        UserProfile profile = profileDao.findByUserCodeAndBannerCode(userCode, bannerCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found for user: " + userCode + ", banner: " + bannerCode));
        return mapToResponse(profile);
    }

    /**
     * Reset user profile pity (usually after maintenance or special event).
     */
    @Transactional
    public UserProfileResponse resetPity(String userCode, String bannerCode) {
        UserProfile profile = profileDao.findByUserCodeAndBannerCode(userCode, bannerCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found for user: " + userCode + ", banner: " + bannerCode));

        log.info("Resetting pity for user {} on banner {}", userCode, bannerCode);
        profile.setPityCounter(0);
        profile.setGuaranteedCounter(0);
        profile.setSoftPityCounter(0);
        profile = profileDao.save(profile);

        return mapToResponse(profile);
    }

    /**
     * Set guaranteed status.
     */
    @Transactional
    public UserProfileResponse setGuaranteed(String userCode, String bannerCode, Boolean guaranteed) {
        UserProfile profile = profileDao.findByUserCodeAndBannerCode(userCode, bannerCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found for user: " + userCode + ", banner: " + bannerCode));

        log.info("Setting guaranteed={} for user {} on banner {}", guaranteed, userCode, bannerCode);
        profile.setIsGuaranteed(guaranteed);
        profile = profileDao.save(profile);

        return mapToResponse(profile);
    }

    /**
     * Get pull statistics for banner.
     */
    public PullStatisticsResponse getStatistics(String userCode, Long bannerId) {
        return gachaEngine.calculateStatistics(userCode, bannerId);
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userCode(profile.getUserCode())
                .bannerCode(profile.getBannerCode())
                .totalPulls(profile.getTotalPulls())
                .pityCounter(profile.getPityCounter())
                .guaranteedCounter(profile.getGuaranteedCounter())
                .isGuaranteed(profile.getIsGuaranteed())
                .lastGuaranteedItemId(profile.getLastGuaranteedItemId())
                .lastFiveStarItemId(profile.getLastFiveStarItemId())
                .softPityCounter(profile.getSoftPityCounter())
                .updatedAt(profile.getUpdatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}
