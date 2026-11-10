package com.example.gacha.gachaengine.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for pull statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullStatisticsResponse {

    private String userCode;
    private String bannerCode;
    private Integer totalPulls;
    private Integer totalFiveStarPulls;
    private Integer totalFourStarPulls;
    private Integer totalThreeStarPulls;
    private Long lastFiveStarPullId;
    private Long lastFourStarPullId;
    private Integer pullsSinceLast5Star;
    private Integer pullsSinceLast4Star;
    private BigDecimal percentageOf5Star;
    private BigDecimal percentageOf4Star;
    private BigDecimal percentageOf3Star;
    private Integer currentPityCounter;
    private Boolean isGuaranteed;
}
