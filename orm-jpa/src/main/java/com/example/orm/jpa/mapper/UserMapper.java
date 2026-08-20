package com.example.orm.jpa.mapper;

import com.example.orm.jpa.dto.DepartmentResponse;
import com.example.orm.jpa.dto.UserResponse;
import com.example.orm.jpa.entity.Department;
import com.example.orm.jpa.entity.User;

import java.util.Collection;
import java.util.List;

/**
 * Entity to response mapping. Kept in one place so no controller ever serializes an entity.
 */
public final class UserMapper {

    private UserMapper() {
    }

    /** Summary: no department access, therefore safe on an entity whose collection is not initialized. */
    public static UserResponse toSummary(User user) {
        return toResponse(user, null);
    }

    /** Detail: expects {@code departmentList} to have been fetch-joined already. */
    public static UserResponse toDetail(User user) {
        return toResponse(user, toDepartments(user.getDepartmentList()));
    }

    /**
     * Detail with departments supplied by a separate query, so the user's own lazy collection is
     * never touched.
     */
    public static UserResponse toDetail(User user, List<DepartmentResponse> departments) {
        return toResponse(user, departments);
    }

    public static DepartmentResponse toDepartment(Department department) {
        return new DepartmentResponse(
                department.getId(), department.getName(), department.getLevels(), department.getOrderNo());
    }

    private static List<DepartmentResponse> toDepartments(Collection<Department> departments) {
        return departments == null
                ? List.of()
                : departments.stream().map(UserMapper::toDepartment).toList();
    }

    private static UserResponse toResponse(User user, List<DepartmentResponse> departments) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getCreateTime(),
                user.getLastUpdateTime(),
                departments);
    }
}
