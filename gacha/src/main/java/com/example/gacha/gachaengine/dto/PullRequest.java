package com.example.gacha.gachaengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for gacha pull.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequest {

    private String userCode;
    private String bannerCode;
    private Integer quantity;
}
