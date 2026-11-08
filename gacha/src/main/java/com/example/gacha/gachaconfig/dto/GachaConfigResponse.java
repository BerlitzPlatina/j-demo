package com.example.gacha.gachaconfig.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A banner's whole gacha configuration.
 *
 * @param editable whether rates and pool may still change (draft, or published but not started)
 * @param ready    whether the configuration is complete enough to publish; {@code issues}
 *                 says what is missing when it is not
 */
public record GachaConfigResponse(
        Long bannerId,
        String bannerCode,
        boolean editable,
        boolean ready,
        List<String> issues,
        BigDecimal totalRate,
        List<RarityRateResponse> rates,
        List<PoolItemResponse> pool) {
}
