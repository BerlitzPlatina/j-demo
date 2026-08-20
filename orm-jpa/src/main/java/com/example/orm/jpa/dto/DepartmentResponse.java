package com.example.orm.jpa.dto;

/**
 * Department as exposed inside a user payload.
 */
public record DepartmentResponse(Long id, String name, Integer levels, Integer orderNo) {
}
