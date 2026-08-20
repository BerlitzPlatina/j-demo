package com.example.orm.jpa.dto;

/**
 * One row of the join table: which user is linked to which department.
 * Carries ids only - the departments themselves are fetched in a separate query and matched
 * back onto these links by {@code departmentId}.
 */
public record UserDepartmentLink(Long userId, Long departmentId) {
}
