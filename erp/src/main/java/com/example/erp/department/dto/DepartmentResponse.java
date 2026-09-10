package com.example.erp.department.dto;

/**
 * Response class for a Department. A record, so the entity itself never reaches
 * Jackson.
 */
public record DepartmentResponse(
                Long id,
                String name,
                String description,
                String departmentCode) {
}
