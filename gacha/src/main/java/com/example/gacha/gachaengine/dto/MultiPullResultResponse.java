package com.example.gacha.gachaengine.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for multi-pull results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiPullResultResponse {

    private String userCode;
    private String bannerCode;
    private Integer totalPulls;
    private List<PullResultResponse> results;
    private PullStatisticsResponse statistics;
    private UserProfileResponse userProfile;
}
