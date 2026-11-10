package com.example.gacha.gachaengine.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gacha.banner.entity.Banner;
import com.example.gacha.banner.repository.BannerDao;
import com.example.gacha.gachaconfig.entity.BannerPoolItem;
import com.example.gacha.gachaconfig.entity.BannerRarityRate;
import com.example.gacha.gachaconfig.repository.BannerPoolItemDao;
import com.example.gacha.gachaconfig.repository.BannerRarityRateDao;
import com.example.gacha.gachaengine.dto.MultiPullResultResponse;
import com.example.gacha.gachaengine.dto.PullRequest;
import com.example.gacha.gachaengine.dto.PullResultResponse;
import com.example.gacha.gachaengine.dto.PullStatisticsResponse;
import com.example.gacha.gachaengine.dto.UserProfileResponse;
import com.example.gacha.gachaengine.entity.PullHistory;
import com.example.gacha.gachaengine.entity.PullType;
import com.example.gacha.gachaengine.entity.UserInventory;
import com.example.gacha.gachaengine.entity.UserProfile;
import com.example.gacha.gachaengine.repository.PullHistoryDao;
import com.example.gacha.gachaengine.repository.UserInventoryDao;
import com.example.gacha.gachaengine.repository.UserProfileDao;
import com.example.gacha.item.entity.Item;
import com.example.gacha.item.repository.ItemDao;
import lombok.extern.slf4j.Slf4j;

/**
 * Gacha Engine: performs random pulls with pity system, guaranteed pulls, and soft pity.
 * 
 * Pity system:
 * - Hard pity at 90 pulls: guarantees a 5-star
 * - Soft pity at 75+ pulls: increased rate-up for featured items
 * - Guaranteed: alternates between featured and non-featured 5-stars
 * - 4-star guaranteed at 10 pulls
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GachaEngine {

    private static final Random RANDOM = new Random();
    
    // Pity constants
    private static final int HARD_PITY_5_STAR = 90;
    private static final int SOFT_PITY_5_STAR = 75;
    private static final int HARD_PITY_4_STAR = 10;

    // Rarity constants
    private static final int RARITY_5_STAR = 5;
    private static final int RARITY_4_STAR = 4;
    private static final int RARITY_3_STAR = 3;

    private final BannerDao bannerDao;
    private final BannerPoolItemDao poolDao;
    private final BannerRarityRateDao rateDao;
    private final ItemDao itemDao;
    private final UserProfileDao profileDao;
    private final PullHistoryDao historyDao;
    private final UserInventoryDao inventoryDao;

    public GachaEngine(BannerDao bannerDao, BannerPoolItemDao poolDao, BannerRarityRateDao rateDao,
            ItemDao itemDao, UserProfileDao profileDao, PullHistoryDao historyDao,
            UserInventoryDao inventoryDao) {
        this.bannerDao = bannerDao;
        this.poolDao = poolDao;
        this.rateDao = rateDao;
        this.itemDao = itemDao;
        this.profileDao = profileDao;
        this.historyDao = historyDao;
        this.inventoryDao = inventoryDao;
    }

    /**
     * Perform a gacha pull (single or multi).
     */
    @Transactional
    public MultiPullResultResponse pull(PullRequest request) {
        Banner banner = bannerDao.findByCode(request.getBannerCode())
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + request.getBannerCode()));

        UserProfile profile = profileDao.findByUserCodeAndBannerId(request.getUserCode(), banner.getId())
                .orElseGet(() -> createUserProfile(request.getUserCode(), banner));

        List<PullResultResponse> results = new ArrayList<>();
        for (int i = 0; i < request.getQuantity(); i++) {
            PullResultResponse result = performSinglePull(profile, banner);
            results.add(result);
        }

        PullStatisticsResponse stats = calculateStatistics(profile.getUserCode(), banner.getId());
        UserProfileResponse profileResponse = mapProfileToResponse(profile);

        return MultiPullResultResponse.builder()
                .userCode(profile.getUserCode())
                .bannerCode(banner.getCode())
                .totalPulls(results.size())
                .results(results)
                .statistics(stats)
                .userProfile(profileResponse)
                .build();
    }

    /**
     * Perform a single pull and return the result.
     */
    private PullResultResponse performSinglePull(UserProfile profile, Banner banner) {
        List<BannerRarityRate> rates = rateDao.findByBannerIdOrderByRarityDesc(banner.getId());
        List<BannerPoolItem> pool = poolDao.findWithItemByBannerId(banner.getId());

        // Determine rarity to pull based on pity system
        int rarityToPull = determineRarity(profile, rates);

        // Get featured items of this rarity
        List<BannerPoolItem> featuredItems = pool.stream()
                .filter(item -> item.getItem().getRarity() == rarityToPull && Boolean.TRUE.equals(item.getFeatured()))
                .toList();

        // Get non-featured items of this rarity
        List<BannerPoolItem> nonFeaturedItems = pool.stream()
                .filter(item -> item.getItem().getRarity() == rarityToPull && !Boolean.TRUE.equals(item.getFeatured()))
                .toList();

        BannerRarityRate rateInfo = rates.stream()
                .filter(r -> r.getRarity() == rarityToPull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No rate info for rarity " + rarityToPull));

        // Decide featured vs non-featured
        boolean isFeatured = decideFeatured(profile, rateInfo, rarityToPull);
        List<BannerPoolItem> itemPool = isFeatured ? featuredItems : nonFeaturedItems;

        // Randomly select item from pool
        BannerPoolItem selectedPoolItem = selectRandomItem(itemPool);
        Item selectedItem = selectedPoolItem.getItem();

        // Update pity counters
        profile.setPityCounter(profile.getPityCounter() + 1);
        profile.setTotalPulls(profile.getTotalPulls() + 1);
        profile.setSoftPityCounter(profile.getSoftPityCounter() + 1);

        // Handle guaranteed system
        if (rarityToPull == RARITY_5_STAR) {
            if (isFeatured && !selectedItem.getId().equals(profile.getLastGuaranteedItemId())) {
                profile.setIsGuaranteed(false);
                profile.setLastGuaranteedItemId(selectedItem.getId());
            } else if (!isFeatured) {
                profile.setIsGuaranteed(true);
            }
            profile.setLastFiveStarItemId(selectedItem.getId());
            profile.setPityCounter(0);
            profile.setSoftPityCounter(0);
        }

        // Save updated profile and history
        profile = profileDao.save(profile);

        PullHistory history = savePullHistory(profile, banner, selectedItem, selectedPoolItem, isFeatured, rarityToPull);

        // Update inventory
        updateInventory(profile.getUserCode(), selectedItem);

        return mapHistoryToResponse(history, selectedItem);
    }

    /**
     * Determine which rarity to pull based on pity system.
     */
    private int determineRarity(UserProfile profile, List<BannerRarityRate> rates) {
        // Hard pity: guarantee 5-star at 90 pulls
        if (profile.getPityCounter() >= HARD_PITY_5_STAR) {
            return RARITY_5_STAR;
        }

        // Soft pity: increased rate-up at 75+
        double roll = RANDOM.nextDouble() * 100;
        
        if (profile.getSoftPityCounter() >= SOFT_PITY_5_STAR) {
            // In soft pity: much higher rate-up
            BannerRarityRate fiveStarRate = rates.stream()
                    .filter(r -> r.getRarity() == RARITY_5_STAR)
                    .findFirst()
                    .orElse(null);
            
            if (fiveStarRate != null && roll < fiveStarRate.getRate().doubleValue() * 3) {
                return RARITY_5_STAR;
            }
        } else {
            // Normal: check base rates
            BannerRarityRate fiveStarRate = rates.stream()
                    .filter(r -> r.getRarity() == RARITY_5_STAR)
                    .findFirst()
                    .orElse(null);
            
            if (fiveStarRate != null && roll < fiveStarRate.getRate().doubleValue()) {
                return RARITY_5_STAR;
            }
        }

        // 4-star or 3-star
        if (profile.getGuaranteedCounter() >= HARD_PITY_4_STAR) {
            return RARITY_4_STAR;
        }

        roll = RANDOM.nextDouble() * 100;
        BannerRarityRate fourStarRate = rates.stream()
                .filter(r -> r.getRarity() == RARITY_4_STAR)
                .findFirst()
                .orElse(null);

        if (fourStarRate != null && roll < fourStarRate.getRate().doubleValue()) {
            profile.setGuaranteedCounter(0);
            return RARITY_4_STAR;
        }

        profile.setGuaranteedCounter(profile.getGuaranteedCounter() + 1);
        return RARITY_3_STAR;
    }

    /**
     * Decide whether to pull featured or non-featured item.
     */
    private boolean decideFeatured(UserProfile profile, BannerRarityRate rateInfo, int rarity) {
        if (rarity != RARITY_5_STAR) {
            double roll = RANDOM.nextDouble() * 100;
            return roll < rateInfo.getFeaturedRate().doubleValue();
        }

        // For 5-star: use guaranteed system
        if (profile.getIsGuaranteed()) {
            return true;
        }

        double roll = RANDOM.nextDouble() * 100;
        return roll < rateInfo.getFeaturedRate().doubleValue();
    }

    /**
     * Randomly select an item from pool based on weights.
     */
    private BannerPoolItem selectRandomItem(List<BannerPoolItem> itemPool) {
        if (itemPool.isEmpty()) {
            throw new IllegalArgumentException("Item pool is empty");
        }

        int totalWeight = itemPool.stream().mapToInt(BannerPoolItem::getWeight).sum();
        int roll = RANDOM.nextInt(totalWeight);

        int current = 0;
        for (BannerPoolItem item : itemPool) {
            current += item.getWeight();
            if (roll < current) {
                return item;
            }
        }

        return itemPool.get(itemPool.size() - 1);
    }

    /**
     * Create user profile if not exists.
     */
    @Transactional
    private UserProfile createUserProfile(String userCode, Banner banner) {
        UserProfile profile = UserProfile.builder()
                .userCode(userCode)
                .bannerId(banner.getId())
                .bannerCode(banner.getCode())
                .totalPulls(0)
                .pityCounter(0)
                .guaranteedCounter(0)
                .isGuaranteed(false)
                .softPityCounter(0)
                .active(true)
                .build();
        return profileDao.save(profile);
    }

    /**
     * Save pull history.
     */
    @Transactional
    private PullHistory savePullHistory(UserProfile profile, Banner banner, Item item, BannerPoolItem poolItem,
            boolean isFeatured, int rarity) {
        PullHistory history = PullHistory.builder()
                .code(UUID.randomUUID().toString())
                .userCode(profile.getUserCode())
                .userProfileId(profile.getId())
                .bannerId(banner.getId())
                .bannerCode(banner.getCode())
                .itemId(item.getId())
                .itemCode(item.getCode())
                .rarity(rarity)
                .isFeatured(isFeatured)
                .pullType(PullType.SINGLE)
                .pityAtPull(profile.getPityCounter())
                .guaranteedAtPull(profile.getGuaranteedCounter())
                .softPityAtPull(profile.getSoftPityCounter())
                .wasGuaranteed(profile.getIsGuaranteed())
                .build();
        return historyDao.save(history);
    }

    /**
     * Update user inventory.
     */
    @Transactional
    private void updateInventory(String userCode, Item item) {
        Optional<UserInventory> existing = inventoryDao.findByUserCodeAndItemId(userCode, item.getId());
        
        if (existing.isPresent()) {
            UserInventory inventory = existing.get();
            inventory.setQuantity(inventory.getQuantity() + 1);
            inventory.setTimesObtained(inventory.getTimesObtained() + 1);
            inventoryDao.save(inventory);
        } else {
            UserInventory inventory = UserInventory.builder()
                    .userCode(userCode)
                    .itemId(item.getId())
                    .itemCode(item.getCode())
                    .quantity(1)
                    .timesObtained(1)
                    .active(true)
                    .build();
            inventoryDao.save(inventory);
        }
    }

    /**
     * Calculate pull statistics for a user on a banner.
     */
    public PullStatisticsResponse calculateStatistics(String userCode, Long bannerId) {
        List<PullHistory> allPulls = historyDao.findByUserCodeAndBannerIdOrderByNewest(userCode, bannerId);
        Optional<UserProfile> profile = profileDao.findByUserCodeAndBannerId(userCode, bannerId);

        int totalPulls = allPulls.size();
        int fiveStarPulls = (int) allPulls.stream().filter(p -> p.getRarity() == RARITY_5_STAR).count();
        int fourStarPulls = (int) allPulls.stream().filter(p -> p.getRarity() == RARITY_4_STAR).count();
        int threeStarPulls = (int) allPulls.stream().filter(p -> p.getRarity() == RARITY_3_STAR).count();

        int pullsSinceLast5Star = 0;
        int pullsSinceLast4Star = 0;
        Long last5StarId = null;
        Long last4StarId = null;

        for (PullHistory pull : allPulls) {
            if (pull.getRarity() == RARITY_5_STAR && last5StarId == null) {
                last5StarId = pull.getId();
                break;
            }
            pullsSinceLast5Star++;
        }

        for (PullHistory pull : allPulls) {
            if (pull.getRarity() == RARITY_4_STAR && last4StarId == null) {
                last4StarId = pull.getId();
                break;
            }
            pullsSinceLast4Star++;
        }

        BigDecimal percentOf5Star = totalPulls > 0
                ? BigDecimal.valueOf(fiveStarPulls).divide(BigDecimal.valueOf(totalPulls), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal percentOf4Star = totalPulls > 0
                ? BigDecimal.valueOf(fourStarPulls).divide(BigDecimal.valueOf(totalPulls), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal percentOf3Star = totalPulls > 0
                ? BigDecimal.valueOf(threeStarPulls).divide(BigDecimal.valueOf(totalPulls), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return PullStatisticsResponse.builder()
                .userCode(userCode)
                .bannerCode(profile.map(UserProfile::getBannerCode).orElse("N/A"))
                .totalPulls(totalPulls)
                .totalFiveStarPulls(fiveStarPulls)
                .totalFourStarPulls(fourStarPulls)
                .totalThreeStarPulls(threeStarPulls)
                .lastFiveStarPullId(last5StarId)
                .lastFourStarPullId(last4StarId)
                .pullsSinceLast5Star(pullsSinceLast5Star)
                .pullsSinceLast4Star(pullsSinceLast4Star)
                .percentageOf5Star(percentOf5Star)
                .percentageOf4Star(percentOf4Star)
                .percentageOf3Star(percentOf3Star)
                .currentPityCounter(profile.map(UserProfile::getPityCounter).orElse(0))
                .isGuaranteed(profile.map(UserProfile::getIsGuaranteed).orElse(false))
                .build();
    }

    /**
     * Map PullHistory to PullResultResponse.
     */
    private PullResultResponse mapHistoryToResponse(PullHistory history, Item item) {
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

    /**
     * Map UserProfile to UserProfileResponse.
     */
    private UserProfileResponse mapProfileToResponse(UserProfile profile) {
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
