package com.example.gacha.gachaengine.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gacha.gachaengine.dto.PullResultResponse;
import com.example.gacha.gachaengine.entity.PullHistory;
import com.example.gacha.gachaengine.repository.PullHistoryDao;
import com.example.gacha.item.repository.ItemDao;
import lombok.extern.slf4j.Slf4j;

/**
 * Pull history management service.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PullHistoryService {

    private final PullHistoryDao historyDao;
    private final ItemDao itemDao;

    public PullHistoryService(PullHistoryDao historyDao, ItemDao itemDao) {
        this.historyDao = historyDao;
        this.itemDao = itemDao;
    }

    /**
     * Get all pull history for a user on a banner.
     */
    public List<PullResultResponse> getPullHistory(String userCode, Long bannerId) {
        return historyDao.findByUserCodeAndBannerIdOrderByNewest(userCode, bannerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get latest pulls for a user.
     */
    public List<PullResultResponse> getLatestPulls(String userCode, Long bannerId, int limit) {
        return historyDao.findLatestByUserCodeAndBannerId(userCode, bannerId, limit).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get all pulls of a specific rarity or higher.
     */
    public List<PullResultResponse> getPullsByRarity(String userCode, Integer minRarity) {
        return historyDao.findByUserCodeAndRarityGreaterOrEqual(userCode, minRarity).stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get total pull count.
     */
    public long getTotalPullCount(String userCode, Long bannerId) {
        return historyDao.countByUserCodeAndBannerId(userCode, bannerId);
    }

    /**
     * Get featured pull count.
     */
    public long getFeaturedPullCount(String userCode, Long bannerId) {
        return historyDao.countFeaturedByUserCodeAndBannerId(userCode, bannerId);
    }

    /**
     * Get pull history for a user across all banners.
     */
    public List<PullResultResponse> getUserPullHistory(String userCode) {
        return historyDao.findByUserCodeOrderByNewest(userCode).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PullResultResponse mapToResponse(PullHistory history) {
        var item = itemDao.findById(history.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + history.getItemId()));

        return PullResultResponse.builder()
                .id(history.getId())
                .code(history.getCode())
                .userCode(history.getUserCode())
                .bannerCode(history.getBannerCode())
                .itemCode(history.getItemCode())
                .itemName(item.getName())
                .rarity(history.getRarity())
                .itemImageUrl(item.getImageUrl())
                .isFeatured(history.getIsFeatured())
                .pullType(history.getPullType().toString())
                .pityAtPull(history.getPityAtPull())
                .guaranteedAtPull(history.getGuaranteedAtPull())
                .softPityAtPull(history.getSoftPityAtPull())
                .wasGuaranteed(history.getWasGuaranteed())
                .pulledAt(history.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}
