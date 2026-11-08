package com.example.gacha.gachaconfig.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.gacha.gachaconfig.entity.BannerPoolItem;
import com.example.gacha.gachaconfig.entity.BannerRarityRate;
import com.example.gacha.item.entity.Item;
import com.example.gacha.item.entity.ItemType;

class GachaConfigEvaluatorTest {

    private static long nextId = 1;

    @Test
    void splitsRarityBetweenFeaturedAndRegularByWeight() {
        List<BannerRarityRate> rates = List.of(rate(5, "0.6", "50"), rate(4, "5.1", "50"), rate(3, "94.3", "0"));
        BannerPoolItem featured5 = pool(item("F5", 5), true, 1);
        BannerPoolItem regular5a = pool(item("R5A", 5), false, 1);
        BannerPoolItem regular5b = pool(item("R5B", 5), false, 3);
        BannerPoolItem featured4 = pool(item("F4", 4), true, 1);
        BannerPoolItem regular4 = pool(item("R4", 4), false, 1);
        BannerPoolItem regular3 = pool(item("R3", 3), false, 1);

        var result = GachaConfigEvaluator.evaluate(rates,
                List.of(featured5, regular5a, regular5b, featured4, regular4, regular3));

        assertThat(result.ready()).isTrue();
        assertThat(result.totalRate()).isEqualByComparingTo("100");
        assertThat(probability(result, featured5)).isEqualByComparingTo("0.3");
        // 0.6 * 50% split 1:3
        assertThat(probability(result, regular5a)).isEqualByComparingTo("0.075");
        assertThat(probability(result, regular5b)).isEqualByComparingTo("0.225");
        assertThat(probability(result, featured4)).isEqualByComparingTo("2.55");
        assertThat(probability(result, regular3)).isEqualByComparingTo("94.3");
    }

    @Test
    void reportsEveryReasonTheConfigCannotBePublished() {
        List<BannerRarityRate> rates = List.of(rate(5, "1", "50"), rate(4, "9", "0"));
        Item inactive = item("OLD4", 4);
        inactive.setActive(false);
        List<BannerPoolItem> pool = List.of(
                pool(item("F5", 5), true, 1),       // 5★ has no non-featured item
                pool(inactive, false, 1),           // inactive
                pool(item("F4", 4), true, 1),       // 4★ featuredRate 0: unreachable
                pool(item("R3", 3), false, 1));     // no 3★ rate: unreachable

        var result = GachaConfigEvaluator.evaluate(rates, pool);

        assertThat(result.ready()).isFalse();
        assertThat(result.issues()).containsExactlyInAnyOrder(
                "rates: total is 10%, must be 100%",
                "pool: 5★ has rate 1% but no non-featured item",
                "pool: item OLD4 is inactive",
                "pool: item F4 can never drop, 4★ featuredRate is 0%",
                "pool: item R3 can never drop, 3★ has no rate");
    }

    @Test
    void emptyConfigIsNotReady() {
        var result = GachaConfigEvaluator.evaluate(List.of(), List.of());
        assertThat(result.issues()).containsExactly("rates: no rarity rates configured", "pool: no items configured");
    }

    private static BigDecimal probability(GachaConfigEvaluator.Evaluation result, BannerPoolItem entry) {
        return result.probabilityByItemId().get(entry.getItem().getId());
    }

    private static BannerRarityRate rate(int rarity, String rate, String featuredRate) {
        return BannerRarityRate.builder().bannerId(1L).rarity(rarity)
                .rate(new BigDecimal(rate)).featuredRate(new BigDecimal(featuredRate)).build();
    }

    private static Item item(String code, int rarity) {
        Item item = Item.builder().code(code).name(code).itemType(ItemType.CHARACTER).rarity(rarity).active(true).build();
        item.setId(nextId++);
        return item;
    }

    private static BannerPoolItem pool(Item item, boolean featured, int weight) {
        return BannerPoolItem.builder().bannerId(1L).item(item).featured(featured).weight(weight).build();
    }
}
