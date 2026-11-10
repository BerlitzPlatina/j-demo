package com.example.gacha.gachaengine.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String userCode;
    private String bannerCode;
    private Integer totalPulls;
    private Integer pityCounter;
    private Integer guaranteedCounter;
    private Boolean isGuaranteed;
    private Long lastGuaranteedItemId;
    private Long lastFiveStarItemId;
    private Integer softPityCounter;
    private LocalDateTime updatedAt;
}
