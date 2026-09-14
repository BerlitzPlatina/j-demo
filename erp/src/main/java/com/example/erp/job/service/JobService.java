package com.example.erp.job.service;

import com.example.common.web.dto.PageResponse;
import com.example.common.web.exception.ResourceNotFoundException;
import com.example.erp.common.page.PageableSupport;
import com.example.erp.common.support.Entities;
import com.example.erp.common.support.Guards;
import com.example.erp.job.dto.JobCreateRequest;
import com.example.erp.job.dto.JobPatchRequest;
import com.example.erp.job.dto.JobResponse;
import com.example.erp.job.dto.JobUpdateRequest;
import com.example.erp.job.entity.Job;
import com.example.erp.job.mapper.JobMapper;
import com.example.erp.job.repository.JobDao;
import com.example.erp.legalentity.entity.LegalEntity;
import com.example.erp.legalentity.repository.LegalEntityDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * CRUD for jobs.
 * <p>
 * Rules the schema cannot express live here: a uniqueness check runs before the
 * insert so the
 * caller gets a 400 naming the field instead of a raw constraint violation, and
 * a sort is
 * validated against an allowlist before it can reach the SQL.
 */
@Service
@Transactional(readOnly = true)
public class JobService {

    /**
     * Properties a client may sort by; anything else is rejected instead of
     * reaching the SQL.
     */
    private static final Set<String> SORTABLE_FIELDS = PageableSupport.sortableFields("jobCode", "jobName", "jobStatus",
            "referenceQuality", "source", "createdBy", "updatedBy");

    /** How this entity is named in a 404 or a duplicate-value message. */
    private static final String ENTITY = "Job";

    private final JobDao jobDao;
    private final LegalEntityDao legalEntityDao;

    public JobService(JobDao jobDao, LegalEntityDao legalEntityDao) {
        this.jobDao = jobDao;
        this.legalEntityDao = legalEntityDao;
    }

    // ------------------------------------------------------------------ read

    /** One page, optionally filtered by a case-insensitive jobCode fragment. */
    public PageResponse<JobResponse> search(String keyword, Pageable pageable) {
        Page<Job> page = jobDao.findByJobCodeContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "",
                PageableSupport.sanitize(pageable, SORTABLE_FIELDS));
        return PageResponse.from(page, JobMapper::toResponse);
    }

    public JobResponse getById(Long id) {
        return JobMapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public JobResponse create(JobCreateRequest request) {
        LegalEntity legalEntityId = resolveLegalEntityId(request.legalEntityIdId());
        Job saved = jobDao.save(JobMapper.toEntity(request, legalEntityId));
        return JobMapper.toResponse(saved);
    }

    /** Full replacement. Fields the caller left out are written as null. */
    @Transactional
    public JobResponse update(Long id, JobUpdateRequest request) {
        Job job = findOrThrow(id);
        LegalEntity legalEntityId = resolveLegalEntityId(request.legalEntityIdId());
        JobMapper.replace(job, request, legalEntityId);
        return JobMapper.toResponse(job);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public JobResponse patch(Long id, JobPatchRequest request) {
        Job job = findOrThrow(id);
        LegalEntity legalEntityId = resolveLegalEntityId(request.legalEntityIdId());
        JobMapper.merge(job, request, legalEntityId);
        return JobMapper.toResponse(job);
    }

    @Transactional
    public void delete(Long id) {
        jobDao.delete(findOrThrow(id));
    }

    // ---------------------------------------------------------------- helper

    private Job findOrThrow(Long id) {
        return Entities.findOrThrow(jobDao, id, ENTITY);
    }

    /**
     * Resolves the relation so a bad id answers 404 here instead of a foreign key
     * error later.
     */
    private LegalEntity resolveLegalEntityId(Long id) {
        if (id == null) {
            return null;
        }
        return legalEntityDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LegalEntity not found with id: " + id));
    }
}
