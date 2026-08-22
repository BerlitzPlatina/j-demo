package com.example.erp.organization.controller;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.erp.organization.dto.OrganizationCreateRequest;
import com.example.erp.organization.dto.OrganizationPatchRequest;
import com.example.erp.organization.dto.OrganizationResponse;
import com.example.erp.organization.dto.OrganizationUpdateRequest;
import com.example.erp.organization.service.OrganizationService;
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
 * Organization CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link OrganizationResponse} transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * GET /api/organizations?keyword=acme&page=0&size=10&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrganizationResponse>>> getOrganizations(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.search(keyword, pageable)));
    }

    /**
     * GET /api/organizations/default
     * <p>
     * Declared before {@code /{id}} matters only for readability - Spring prefers the literal
     * path over the variable one either way.
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getDefaultOrganization() {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getDefault()));
    }

    /**
     * GET /api/organizations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.getById(id)));
    }

    /**
     * POST /api/organizations
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
            @Valid @RequestBody OrganizationCreateRequest request) {
        OrganizationResponse created = organizationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT /api/organizations/{id} - full replacement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> replaceOrganization(
            @PathVariable Long id, @Valid @RequestBody OrganizationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.update(id, request)));
    }

    /**
     * PATCH /api/organizations/{id} - only the fields present in the body are changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> patchOrganization(
            @PathVariable Long id, @Valid @RequestBody OrganizationPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(organizationService.patch(id, request)));
    }

    /**
     * DELETE /api/organizations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable Long id) {
        organizationService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
