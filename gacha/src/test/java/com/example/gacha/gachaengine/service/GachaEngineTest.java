package com.example.gacha.gachaengine.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.gacha.banner.entity.Banner;
import com.example.gacha.banner.entity.BannerStatus;
import com.example.gacha.banner.entity.BannerType;
import com.example.gacha.banner.repository.BannerDao;
import com.example.gacha.gachaconfig.entity.BannerPoolItem;
import com.example.gacha.gachaconfig.entity.BannerRarityRate;
import com.example.gacha.gachaconfig.repository.BannerPoolItemDao;
import com.example.gacha.gachaconfig.repository.BannerRarityRateDao;
import com.example.gacha.gachaengine.dto.PullRequest;
import com.example.gacha.gachaengine.entity.UserProfile;
import com.example.gacha.gachaengine.repository.PullHistoryDao;
import com.example.gacha.gachaengine.repository.UserInventoryDao;
import com.example.gacha.gachaengine.repository.UserProfileDao;
import com.example.gacha.item.entity.Item;
import com.example.gacha.item.entity.ItemType;
import com.example.gacha.item.repository.ItemDao;

/**
 * Unit tests for GachaEngine service.
 */
@ExtendWith(MockitoExtension.class)
class GachaEngineTest {

    @Mock
    private BannerDao bannerDao;

    @Mock
    private BannerPoolItemDao poolDao;

    @Mock
    private BannerRarityRateDao rateDao;

    @Mock
    private ItemDao itemDao;

    @Mock
    private UserProfileDao profileDao;

    @Mock
    private PullHistoryDao historyDao;

    @Mock
    private UserInventoryDao inventoryDao;

    @InjectMocks
    private GachaEngine gachaEngine;

    private Banner testBanner;
    private Item fiveStarItem;

    @BeforeEach
    void setUp() {
        testBanner = Banner.builder()
                .code("test_banner_001")
                .name("Test Banner")
                .bannerType(BannerType.LIMITED)
                .status(BannerStatus.PUBLISHED)
                .build();
        testBanner.setId(1L);

        fiveStarItem = Item.builder()
                .code("item_5star_001")
                .name("5-Star Character")
                .rarity(5)
                .itemType(ItemType.CHARACTER)
                .active(true)
                .build();
        fiveStarItem.setId(1L);
    }

    @Test
    void testPullWithInvalidBanner() {
        when(bannerDao.findByCode("invalid_banner"))
                .thenReturn(Optional.empty());
        
        PullRequest request = PullRequest.builder()
                .userCode("user001")
                .bannerCode("invalid_banner")
                .quantity(1)
                .build();
        
        assertThrows(IllegalArgumentException.class, () -> gachaEngine.pull(request));
    }

    @Test
    void testCalculateStatistics() {
        String userCode = "test_user_001";
        Long bannerId = 1L;
        
        when(historyDao.findByUserCodeAndBannerIdOrderByNewest(userCode, bannerId))
                .thenReturn(List.of());
        
        when(profileDao.findByUserCodeAndBannerId(userCode, bannerId))
                .thenReturn(Optional.empty());
        
        var stats = gachaEngine.calculateStatistics(userCode, bannerId);
        
        assertNotNull(stats);
        assertEquals(0, stats.getTotalPulls());
        assertEquals(0, stats.getTotalFiveStarPulls());
    }

    @Test
    void testProfileCreation() {
        UserProfile profile = UserProfile.builder()
                .userCode("user001")
                .bannerId(1L)
                .bannerCode("banner001")
                .totalPulls(0)
                .pityCounter(0)
                .guaranteedCounter(0)
                .isGuaranteed(false)
                .softPityCounter(0)
                .active(true)
                .build();
        
        assertNotNull(profile);
        assertEquals("user001", profile.getUserCode());
        assertEquals(0, profile.getPityCounter());
        assertEquals(false, profile.getIsGuaranteed());
    }
}

