package com.example.gacha.gachaconfig.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gacha.banner.entity.Banner;
import com.example.gacha.banner.entity.BannerStatus;
import com.example.gacha.banner.repository.BannerDao;
import com.example.gacha.common.support.Entities;
import com.example.gacha.gachaconfig.dto.GachaConfigResponse;
import com.example.gacha.gachaconfig.dto.PoolEntry;
import com.example.gacha.gachaconfig.dto.PoolItemResponse;
import com.example.gacha.gachaconfig.dto.PoolRequest;
import com.example.gacha.gachaconfig.dto.RarityRateEntry;
import com.example.gacha.gachaconfig.dto.RarityRateResponse;
import com.example.gacha.gachaconfig.dto.RarityRatesRequest;
import com.example.gacha.gachaconfig.entity.BannerPoolItem;
import com.example.gacha.gachaconfig.entity.BannerRarityRate;
import com.example.gacha.gachaconfig.repository.BannerPoolItemDao;
import com.example.gacha.gachaconfig.repository.BannerRarityRateDao;
import com.example.gacha.item.entity.Item;
import com.example.gacha.item.repository.ItemDao;

/**
 * Gacha configuration of a banner: its rarity rate table and its item pool.
 * <p>
 * Both are replaced as a whole (PUT), which keeps every write atomic and lets the rate table
 * insist on adding up to 100 at all times. Consistency between the two - every rollable rarity
 * has items, every item can drop - is not enforced on write, since they are edited one after
 * the other; it is reported by {@link #getConfig} and enforced when the banner is published.
 * <p>
 * Once a published banner has started, the configuration is locked: players have pulled under
 * the published odds.
 */
@Service
@Transactional(readOnly = true)
public class GachaConfigService {

    private final BannerDao bannerDao;
    private final BannerRarityRateDao rateDao;
    private final BannerPoolItemDao poolDao;
    private final ItemDao itemDao;

    public GachaConfigService(BannerDao bannerDao, BannerRarityRateDao rateDao, BannerPoolItemDao poolDao,
            ItemDao itemDao) {
        this.bannerDao = bannerDao;
        this.rateDao = rateDao;
        this.poolDao = poolDao;
        this.itemDao = itemDao;
    }

    // ------------------------------------------------------------------ read

    public GachaConfigResponse getConfig(Long bannerId) {
        return toResponse(findBanner(bannerId));
    }

    /**
     * Called by the banner publish: refuses with every outstanding issue at once, so the
     * operator can fix them in one go.
     */
    public void assertReady(Long bannerId) {
        GachaConfigEvaluator.Evaluation evaluation = GachaConfigEvaluator.evaluate(
                rateDao.findByBannerIdOrderByRarityDesc(bannerId), poolDao.findWithItemByBannerId(bannerId));
        if (!evaluation.ready()) {
            throw new IllegalArgumentException("config: the banner is not ready to publish - "
                    + String.join("; ", evaluation.issues()));
        }
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public GachaConfigResponse replaceRates(Long bannerId, RarityRatesRequest request) {
        assertEditable(findBanner(bannerId));

        Set<Integer> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RarityRateEntry entry : request.rates()) {
            if (!seen.add(entry.rarity())) {
                throw new IllegalArgumentException("rates: rarity " + entry.rarity() + " appears more than once");
            }
            total = total.add(entry.rate());
        }
        if (!request.rates().isEmpty() && total.compareTo(GachaConfigEvaluator.HUNDRED) != 0) {
            throw new IllegalArgumentException(
                    "rates: total is " + total.stripTrailingZeros().toPlainString() + "%, must be 100%");
        }

        rateDao.deleteAllByBannerId(bannerId);
        rateDao.saveAll(request.rates().stream()
                .map(entry -> BannerRarityRate.builder()
                        .bannerId(bannerId)
                        .rarity(entry.rarity())
                        .rate(entry.rate())
                        .featuredRate(entry.featuredRate() != null ? entry.featuredRate() : BigDecimal.ZERO)
                        .build())
                .toList());
        rateDao.flush();
        return getConfig(bannerId);
    }

    @Transactional
    public GachaConfigResponse replacePool(Long bannerId, PoolRequest request) {
        assertEditable(findBanner(bannerId));

        Set<Long> ids = new HashSet<>();
        for (PoolEntry entry : request.items()) {
            if (!ids.add(entry.itemId())) {
                throw new IllegalArgumentException("items: item " + entry.itemId() + " appears more than once");
            }
        }
        Map<Long, Item> items = itemDao.findAllById(ids).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        List<Long> missing = ids.stream().filter(id -> !items.containsKey(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("items: item(s) not found: " + missing);
        }
        List<String> inactive = items.values().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getActive()))
                .map(Item::getCode).sorted().toList();
        if (!inactive.isEmpty()) {
            throw new IllegalArgumentException("items: inactive item(s) cannot be added: " + inactive);
        }

        poolDao.deleteAllByBannerId(bannerId);
        poolDao.saveAll(request.items().stream()
                .map(entry -> BannerPoolItem.builder()
                        .bannerId(bannerId)
                        // The bulk delete cleared the persistence context; re-attach by reference.
                        .item(itemDao.getReferenceById(entry.itemId()))
                        .featured(Boolean.TRUE.equals(entry.featured()))
                        .weight(entry.weight() != null ? entry.weight() : 1)
                        .build())
                .toList());
        poolDao.flush();
        return getConfig(bannerId);
    }

    // ---------------------------------------------------------------- helper

    private Banner findBanner(Long bannerId) {
        return Entities.findOrThrow(bannerDao, bannerId, "Banner");
    }

    private static boolean isEditable(Banner banner, LocalDateTime now) {
        return banner.getStatus() != BannerStatus.ARCHIVED && !banner.hasStartedAt(now);
    }

    private static void assertEditable(Banner banner) {
        if (banner.getStatus() == BannerStatus.ARCHIVED) {
            throw new IllegalArgumentException("status: an ARCHIVED banner cannot be edited");
        }
        if (banner.hasStartedAt(LocalDateTime.now())) {
            throw new IllegalArgumentException("status: the banner is live, its gacha configuration can no longer change");
        }
    }

    private GachaConfigResponse toResponse(Banner banner) {
        List<BannerRarityRate> rates = rateDao.findByBannerIdOrderByRarityDesc(banner.getId());
        List<BannerPoolItem> pool = poolDao.findWithItemByBannerId(banner.getId());
        GachaConfigEvaluator.Evaluation evaluation = GachaConfigEvaluator.evaluate(rates, pool);

        return new GachaConfigResponse(
                banner.getId(),
                banner.getCode(),
                isEditable(banner, LocalDateTime.now()),
                evaluation.ready(),
                evaluation.issues(),
                evaluation.totalRate(),
                rates.stream()
                        .map(rate -> new RarityRateResponse(rate.getRarity(), rate.getRate(), rate.getFeaturedRate()))
                        .toList(),
                pool.stream()
                        .map(entry -> {
                            Item item = entry.getItem();
                            return new PoolItemResponse(item.getId(), item.getCode(), item.getName(),
                                    item.getItemType(), item.getRarity(), item.getActive(), entry.getFeatured(),
                                    entry.getWeight(), evaluation.probabilityByItemId().get(item.getId()));
                        })
                        .toList());
    }
}
