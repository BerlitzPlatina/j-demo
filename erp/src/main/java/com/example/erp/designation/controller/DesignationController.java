package com.example.erp.designation.controller;

import com.example.common.web.dto.ApiResponse;

import com.example.common.web.dto.PageResponse;
import com.example.erp.common.constant.ApiPaths;
import com.example.erp.common.constant.ErpConstants;
import com.example.erp.designation.dto.DesignationCreateRequest;
import com.example.erp.designation.dto.DesignationPatchRequest;
import com.example.erp.designation.dto.DesignationResponse;
import com.example.erp.designation.dto.DesignationUpdateRequest;
import com.example.erp.designation.service.DesignationService;
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
 * Designation CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link DesignationResponse} transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping(ApiPaths.DESIGNATIONS)
public class DesignationController {

    private final DesignationService designationService;

    public DesignationController(DesignationService designationService) {
        this.designationService = designationService;
    }

    /**
     * GET /api/designations?keyword=abc&page=0&size=10&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DesignationResponse>>> getDesignations(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = ErpConstants.DEFAULT_PAGE_SIZE, sort = ErpConstants.ID,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(designationService.search(keyword, pageable)));
    }

    /**
     * GET /api/designations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DesignationResponse>> getDesignation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(designationService.getById(id)));
    }

    /**
     * POST /api/designations
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DesignationResponse>> createDesignation(
            @Valid @RequestBody DesignationCreateRequest request) {
        DesignationResponse created = designationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT /api/designations/{id} - full replacement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DesignationResponse>> replaceDesignation(
            @PathVariable Long id, @Valid @RequestBody DesignationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(designationService.update(id, request)));
    }

    /**
     * PATCH /api/designations/{id} - only the fields present in the body are changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<DesignationResponse>> patchDesignation(
            @PathVariable Long id, @Valid @RequestBody DesignationPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(designationService.patch(id, request)));
    }

    /**
     * DELETE /api/designations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDesignation(@PathVariable Long id) {
        designationService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
