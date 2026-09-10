package com.example.erp.department.controller;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.erp.department.dto.DepartmentCreateRequest;
import com.example.erp.department.dto.DepartmentPatchRequest;
import com.example.erp.department.dto.DepartmentResponse;
import com.example.erp.department.dto.DepartmentUpdateRequest;
import com.example.erp.department.service.DepartmentService;
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
 * Department CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link DepartmentResponse} transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * GET /api/departments?keyword=abc&page=0&size=10&sort=name,asc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> getDepartments(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.search(keyword, pageable)));
    }

    /**
     * GET /api/departments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getById(id)));
    }

    /**
     * POST /api/departments
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentResponse created = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT /api/departments/{id} - full replacement.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> replaceDepartment(
            @PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.update(id, request)));
    }

    /**
     * PATCH /api/departments/{id} - only the fields present in the body are changed.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> patchDepartment(
            @PathVariable Long id, @Valid @RequestBody DepartmentPatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.patch(id, request)));
    }

    /**
     * DELETE /api/departments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
