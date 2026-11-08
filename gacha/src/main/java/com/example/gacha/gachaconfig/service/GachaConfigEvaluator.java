package com.example.gacha.gachaconfig.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.gacha.gachaconfig.entity.BannerPoolItem;
import com.example.gacha.gachaconfig.entity.BannerRarityRate;
import com.example.gacha.item.entity.Item;

/**
 * The pure arithmetic of a banner configuration: per-item probabilities and the list of reasons
 * it cannot be published yet. No database, so it is unit-tested on its own.
 * <p>
 * Probability of an item, in percent:
 *
 * <pre>
 * rate(rarity) * share / 100 * weight / totalWeight(rarity, featured)
 *   where share = featuredRate        for a featured item
 *               = 100 - featuredRate  otherwise
 * </pre>
 */
public final class GachaConfigEvaluator {

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Probabilities are reported with 6 decimals: a 0.6% rarity split over many items is tiny. */
    static final int PROBABILITY_SCALE = 6;

    private GachaConfigEvaluator() {
    }

    /**
     * @param issues              empty when the configuration can be published
     * @param probabilityByItemId per item id, in percent; 0 for an item that can never drop
     */
    public record Evaluation(BigDecimal totalRate, List<String> issues, Map<Long, BigDecimal> probabilityByItemId) {

        public boolean ready() {
            return issues.isEmpty();
        }
    }

    private record Group(int rarity, boolean featured) {
    }

    /** The pool items must have their {@link Item} loaded. */
    public static Evaluation evaluate(List<BannerRarityRate> rates, List<BannerPoolItem> pool) {
        List<String> issues = new ArrayList<>();

        Map<Integer, BannerRarityRate> rateByRarity = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (BannerRarityRate rate : rates) {
            rateByRarity.put(rate.getRarity(), rate);
            total = total.add(rate.getRate());
        }
        if (rates.isEmpty()) {
            issues.add("rates: no rarity rates configured");
        } else if (total.compareTo(HUNDRED) != 0) {
            issues.add("rates: total is " + total.stripTrailingZeros().toPlainString() + "%, must be 100%");
        }
        if (pool.isEmpty()) {
            issues.add("pool: no items configured");
        }

        Map<Group, Integer> weightByGroup = new HashMap<>();
        for (BannerPoolItem entry : pool) {
            weightByGroup.merge(groupOf(entry), entry.getWeight(), Integer::sum);
        }

        // Every rarity that can be rolled needs something to land on.
        for (BannerRarityRate rate : rates) {
            if (rate.getRate().signum() == 0) {
                continue;
            }
            int rarity = rate.getRarity();
            if (rate.getFeaturedRate().signum() > 0 && !weightByGroup.containsKey(new Group(rarity, true))) {
                issues.add("pool: " + rarity + "★ has featuredRate " + percent(rate.getFeaturedRate())
                        + " but no featured item");
            }
            if (rate.getFeaturedRate().compareTo(HUNDRED) < 0 && !weightByGroup.containsKey(new Group(rarity, false))) {
                issues.add("pool: " + rarity + "★ has rate " + percent(rate.getRate())
                        + " but no non-featured item");
            }
        }

        Map<Long, BigDecimal> probabilities = new HashMap<>();
        for (BannerPoolItem entry : pool) {
            Item item = entry.getItem();
            if (!Boolean.TRUE.equals(item.getActive())) {
                issues.add("pool: item " + item.getCode() + " is inactive");
            }
            BannerRarityRate rate = rateByRarity.get(item.getRarity());
            if (rate == null || rate.getRate().signum() == 0) {
                issues.add("pool: item " + item.getCode() + " can never drop, " + item.getRarity()
                        + "★ has no rate");
                probabilities.put(item.getId(), BigDecimal.ZERO.setScale(PROBABILITY_SCALE));
                continue;
            }
            BigDecimal share = Boolean.TRUE.equals(entry.getFeatured())
                    ? rate.getFeaturedRate()
                    : HUNDRED.subtract(rate.getFeaturedRate());
            if (share.signum() == 0) {
                issues.add("pool: item " + item.getCode() + " can never drop, " + item.getRarity()
                        + "★ featuredRate is " + percent(rate.getFeaturedRate()));
            }
            BigDecimal probability = rate.getRate()
                    .multiply(share)
                    .multiply(BigDecimal.valueOf(entry.getWeight()))
                    .divide(HUNDRED.multiply(BigDecimal.valueOf(weightByGroup.get(groupOf(entry)))),
                            PROBABILITY_SCALE, RoundingMode.HALF_UP);
            probabilities.put(item.getId(), probability);
        }

        return new Evaluation(total, List.copyOf(issues), Map.copyOf(probabilities));
    }

    private static Group groupOf(BannerPoolItem entry) {
        return new Group(entry.getItem().getRarity(), Boolean.TRUE.equals(entry.getFeatured()));
    }

    private static String percent(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString() + "%";
    }
}
