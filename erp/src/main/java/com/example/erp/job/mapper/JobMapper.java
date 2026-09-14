package com.example.erp.job.mapper;

import com.example.erp.job.dto.JobCreateRequest;
import com.example.erp.job.dto.JobPatchRequest;
import com.example.erp.job.dto.JobResponse;
import com.example.erp.job.dto.JobUpdateRequest;
import com.example.erp.job.entity.Job;
import com.example.erp.legalentity.entity.LegalEntity;

/**
 * Entity to response, and request to entity. Kept in one place so no controller
 * ever serializes
 * an entity and no service hand-copies fields.
 */
public final class JobMapper {

    private JobMapper() {
    }

    public static JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getLegalEntity() == null ? null : job.getLegalEntity().getId(),
                job.getJobCode(),
                job.getJobName(),
                job.getJobStatus(),
                job.getReferenceQuality(),
                job.getSource(),
                job.getCreatedBy(),
                job.getUpdatedBy(),
                job.getCreateTime(),
                job.getLastUpdateTime());
    }

    public static Job toEntity(JobCreateRequest request, LegalEntity legalEntity) {
        return Job.builder()
                .legalEntity(legalEntity)
                .jobCode(request.jobCode())
                .jobName(request.jobName())
                .jobStatus(request.jobStatus())
                .referenceQuality(request.referenceQuality())
                .source(request.source())
                .createdBy(request.createdBy())
                .updatedBy(request.updatedBy())
                .build();
    }

    /** Full replacement: an omitted optional field is written as null. */
    public static void replace(Job target, JobUpdateRequest request, LegalEntity legalEntity) {
        target.setLegalEntity(legalEntity);
        target.setJobCode(request.jobCode());
        target.setJobName(request.jobName());
        target.setJobStatus(request.jobStatus());
        target.setReferenceQuality(request.referenceQuality());
        target.setSource(request.source());
        target.setCreatedBy(request.createdBy());
        target.setUpdatedBy(request.updatedBy());
    }

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge(Job target, JobPatchRequest request, LegalEntity legalEntity) {
        if (legalEntity != null) {
            target.setLegalEntity(legalEntity);
        }
        if (request.jobCode() != null) {
            target.setJobCode(request.jobCode());
        }
        if (request.jobName() != null) {
            target.setJobName(request.jobName());
        }
        if (request.jobStatus() != null) {
            target.setJobStatus(request.jobStatus());
        }
        if (request.referenceQuality() != null) {
            target.setReferenceQuality(request.referenceQuality());
        }
        if (request.source() != null) {
            target.setSource(request.source());
        }
        if (request.createdBy() != null) {
            target.setCreatedBy(request.createdBy());
        }
        if (request.updatedBy() != null) {
            target.setUpdatedBy(request.updatedBy());
        }
    }
}
