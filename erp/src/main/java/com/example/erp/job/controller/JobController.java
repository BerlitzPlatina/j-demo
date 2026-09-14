package com.example.erp.job.controller;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.erp.common.constant.ApiPaths;
import com.example.erp.common.constant.ErpConstants;
import com.example.erp.job.dto.JobCreateRequest;
import com.example.erp.job.dto.JobPatchRequest;
import com.example.erp.job.dto.JobResponse;
import com.example.erp.job.dto.JobUpdateRequest;
import com.example.erp.job.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Job CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link JobResponse} transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping(ApiPaths.JOBS)
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * GET /api/jobs?keyword=abc&page=0&size=10&sort=jobCode,asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> getJobs(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = ErpConstants.DEFAULT_PAGE_SIZE, sort = ErpConstants.ID,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(jobService.search(keyword, pageable)));
    }

    /**
     * GET /api/jobs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getById(id)));
    }

    /**
     * POST /api/jobs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobCreateRequest request) {
        JobResponse created = jobService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT /api/jobs/{id} - full replacement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> replaceJob(
            @PathVariable Long id, @Valid @RequestBody JobUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(jobService.update(id, request)));
    }

    /**
     * PATCH /api/jobs/{id} - only the fields present in the body are changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> patchJob(
            @PathVariable Long id, @Valid @RequestBody JobPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(jobService.patch(id, request)));
    }

    /**
     * DELETE /api/jobs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
