package com.example.erp.designation.mapper;

import com.example.erp.designation.dto.DesignationCreateRequest;
import com.example.erp.designation.dto.DesignationPatchRequest;
import com.example.erp.designation.dto.DesignationResponse;
import com.example.erp.designation.dto.DesignationUpdateRequest;
import com.example.erp.designation.entity.Designation;

/**
 * Entity to response, and request to entity. Kept in one place so no controller ever serializes
 * an entity and no service hand-copies fields.
 */
public final class DesignationMapper {

    private DesignationMapper() {
    }

    public static DesignationResponse toResponse(Designation designation) {
        return new DesignationResponse(
                designation.getId(),
                designation.getName(),
                designation.getCreateTime(),
                designation.getLastUpdateTime());
    }

    public static Designation toEntity(DesignationCreateRequest request) {
        return Designation.builder()
                .name(request.name())
                .build();
    }

    /** Full replacement: an omitted optional field is written as null. */
    public static void replace(Designation target, DesignationUpdateRequest request) {
        target.setName(request.name());
    }

    /** Partial update: only the fields the caller actually sent are copied over. */
    public static void merge(Designation target, DesignationPatchRequest request) {
        if (request.name() != null) {
            target.setName(request.name());
        }
    }
}
