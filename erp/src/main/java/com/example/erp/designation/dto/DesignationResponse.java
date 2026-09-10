package com.example.erp.designation.dto;

import java.util.Date;

/**
 * Response class for a Designation. A record, so the entity itself never reaches Jackson.
 */
public record DesignationResponse(
        Long id,
        String name,
        Date createTime,
        Date lastUpdateTime) {
}
