package com.example.erp.job.dto;

import java.util.Date;

/**
 * Response class for a Job. A record, so the entity itself never reaches Jackson.
 */
public record JobResponse(
        Long id,
        Long legalEntityIdId,
        String jobCode,
        String jobName,
        String jobStatus,
        String referenceQuality,
        String source,
        Integer createdBy,
        Integer updatedBy,
        Date createTime,
        Date lastUpdateTime) {
}
