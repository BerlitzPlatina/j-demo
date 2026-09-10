package com.example.erp.department.mapper;

import com.example.erp.department.dto.DepartmentCreateRequest;
import com.example.erp.department.dto.DepartmentPatchRequest;
import com.example.erp.department.dto.DepartmentResponse;
import com.example.erp.department.dto.DepartmentUpdateRequest;
import com.example.erp.department.entity.Department;

/**
 * Entity to response, and request to entity. Kept in one place so no controller
 * ever serializes
 * an entity and no service hand-copies fields.
 */
public final class DepartmentMapper {

    private DepartmentMapper() {
    }

    public static DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getDepartmentCode());
    }

    public static Department toEntity(DepartmentCreateRequest request) {
        return Department.builder()
                .name(request.name())
                .description(request.description())
                .departmentCode(request.departmentCode())
                .build();
    }

    /** Full replacement: an omitted optional field is written as null. */
    public static void replace(Department target, DepartmentUpdateRequest request) {
        target.setName(request.name());
        target.setDescription(request.description());
        target.setDepartmentCode(request.departmentCode());
    }

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge(Department target, DepartmentPatchRequest request) {
        if (request.name() != null) {
            target.setName(request.name());
        }
        if (request.description() != null) {
            target.setDescription(request.description());
        }
        if (request.departmentCode() != null) {
            target.setDepartmentCode(request.departmentCode());
        }
    }
}
