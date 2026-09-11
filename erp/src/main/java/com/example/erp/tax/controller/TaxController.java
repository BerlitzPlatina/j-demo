package com.example.erp.tax.controller;

import com.example.common.web.dto.ApiResponse;

import com.example.common.web.dto.PageResponse;
import com.example.erp.common.constant.ApiPaths;
import com.example.erp.common.constant.ErpConstants;
import com.example.erp.tax.dto.TaxCreateRequest;
import com.example.erp.tax.dto.TaxPatchRequest;
import com.example.erp.tax.dto.TaxResponse;
import com.example.erp.tax.dto.TaxSearchRequest;
import com.example.erp.tax.dto.TaxUpdateRequest;
import com.example.erp.tax.service.TaxService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tax CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link TaxResponse} transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping(ApiPaths.TAXES)
public class TaxController {

    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    /**
     * GET /api/settings/taxes?keyword=vat&type=GST&startDateFrom=2026-01-01
     * &page=0&size=10&sort=name,asc
     * <p>
     * The filters arrive as one {@code @ModelAttribute} rather than a parameter each, so adding
     * one is a field on {@link TaxSearchRequest} and a line in {@code TaxSpecifications}, not a
     * change to this signature. An absent parameter stays null and is dropped from the query.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TaxResponse>>> getTaxes(
            @ModelAttribute TaxSearchRequest request,
            @PageableDefault(size = ErpConstants.DEFAULT_PAGE_SIZE, sort = ErpConstants.ID,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(taxService.search(request, pageable)));
    }

    /**
     * GET /api/settings/taxes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxResponse>> getTax(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taxService.getById(id)));
    }

    /**
     * POST /api/settings/taxes
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TaxResponse>> createTax(
            @Valid @RequestBody TaxCreateRequest request) {
        TaxResponse created = taxService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT /api/settings/taxes/{id} - full replacement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxResponse>> replaceTax(
            @PathVariable Long id, @Valid @RequestBody TaxUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taxService.update(id, request)));
    }

    /**
     * PATCH /api/settings/taxes/{id} - only the fields present in the body are changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TaxResponse>> patchTax(
            @PathVariable Long id, @Valid @RequestBody TaxPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taxService.patch(id, request)));
    }

    /**
     * DELETE /api/settings/taxes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTax(@PathVariable Long id) {
        taxService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
