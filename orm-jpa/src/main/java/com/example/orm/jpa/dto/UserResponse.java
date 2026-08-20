package com.example.orm.jpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;

/**
 * Response class for a user. Never exposes {@code password} / {@code salt},
 * and carries {@code departments} only when the caller asked for them
 * (the fetch join is optional, so the field is omitted when null).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        Integer status,
        Date lastLoginTime,
        Date createTime,
        Date lastUpdateTime,
        List<DepartmentResponse> departments) {
}
