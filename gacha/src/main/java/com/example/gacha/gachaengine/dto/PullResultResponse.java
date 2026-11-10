package com.example.gacha.gachaengine.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for pull result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullResultResponse {

    private Long id;
    private String code;
    private String userCode;
    private String bannerCode;
    private String itemCode;
    private String itemName;
    private Integer rarity;
    private String itemImageUrl;
    private Boolean isFeatured;
    private String pullType;
    private Integer pityAtPull;
    private Integer guaranteedAtPull;
    private Integer softPityAtPull;
    private Boolean wasGuaranteed;
    private LocalDateTime pulledAt;
}
