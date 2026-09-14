package com.example.erp.legalentity.controller;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.erp.common.constant.ApiPaths;
import com.example.erp.common.constant.ErpConstants;
import com.example.erp.legalentity.dto.LegalEntityCreateRequest;
import com.example.erp.legalentity.dto.LegalEntityPatchRequest;
import com.example.erp.legalentity.dto.LegalEntityResponse;
import com.example.erp.legalentity.dto.LegalEntityUpdateRequest;
import com.example.erp.legalentity.service.LegalEntityService;
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
 * LegalEntity CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link LegalEntityResponse} transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping(ApiPaths.LEGAL_ENTITIES)
public class LegalEntityController {

    private final LegalEntityService legalEntityService;

    public LegalEntityController(LegalEntityService legalEntityService) {
        this.legalEntityService = legalEntityService;
    }

    /**
     * GET /api/legal-entities?keyword=abc&page=0&size=10&sort=entityCode,asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LegalEntityResponse>>> getLegalEntities(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = ErpConstants.DEFAULT_PAGE_SIZE, sort = ErpConstants.ID,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(legalEntityService.search(keyword, pageable)));
    }

    /**
     * GET /api/legal-entities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> getLegalEntity(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(legalEntityService.getById(id)));
    }

    /**
     * POST /api/legal-entities
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LegalEntityResponse>> createLegalEntity(
            @Valid @RequestBody LegalEntityCreateRequest request) {
        LegalEntityResponse created = legalEntityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT /api/legal-entities/{id} - full replacement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> replaceLegalEntity(
            @PathVariable Long id, @Valid @RequestBody LegalEntityUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(legalEntityService.update(id, request)));
    }

    /**
     * PATCH /api/legal-entities/{id} - only the fields present in the body are changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<LegalEntityResponse>> patchLegalEntity(
            @PathVariable Long id, @Valid @RequestBody LegalEntityPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(legalEntityService.patch(id, request)));
    }

    /**
     * DELETE /api/legal-entities/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLegalEntity(@PathVariable Long id) {
        legalEntityService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
