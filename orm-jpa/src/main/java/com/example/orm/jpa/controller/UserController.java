package com.example.orm.jpa.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.orm.jpa.dto.ApiResponse;
import com.example.orm.jpa.dto.UserInclude;
import com.example.orm.jpa.dto.UserResponse;
import com.example.orm.jpa.entity.User;
import com.example.orm.jpa.mapper.UserMapper;
import com.example.orm.jpa.repository.UserDao;

/**
 * Unpaged user listing, straight off the repository.
 * <p>
 * No paging means no {@code limit}, and without a {@code limit} a fetch join is
 * the simplest
 * correct answer: one query, no N+1, nothing to stitch in memory. The
 * split-query machinery in
 * {@code UserService} only exists because paging and fetch joins do not mix.
 * <p>
 * The one thing that does not change: the entity stops here. Returning
 * {@code List<User>} would
 * put the password and salt in the response and let Jackson walk
 * {@code Department.superior <-> children} in circles.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserDao userDao;

    UserController(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * GET /users?include=departments
     * <p>
     * Returns every user. Fine for a demo and for reference data; on a table that
     * keeps growing,
     * prefer the paged {@code /api/users}, since this response has no upper bound.
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) Set<String> include) {

        if (UserInclude.parse(include).contains(UserInclude.DEPARTMENTS)) {
            // One query: select users left join fetch orm_user_dept join orm_department.
            return ResponseEntity.ok(ApiResponse.success(
                    userDao.findAllWithDepartments().stream().map(UserMapper::toDetail).toList()));
        }
        // One query on orm_user; the lazy collection is never touched.
        return ResponseEntity.ok(ApiResponse.success(
                userDao.findAll().stream().map(UserMapper::toSummary).toList()));
    }

}
