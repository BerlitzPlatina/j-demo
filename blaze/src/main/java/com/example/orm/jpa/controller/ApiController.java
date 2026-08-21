package com.example.orm.jpa.controller;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.orm.jpa.service.UserService;
import com.example.orm.jpa.view.UserDetailView;
import com.example.orm.jpa.view.UserSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * User read API.
 */
@RestController
@RequestMapping("/api/users")
public class ApiController {

    /** Value accepted by the {@code include} query parameter. */
    private static final String INCLUDE_DEPARTMENTS = "departments";

    private final UserService userService;

    public ApiController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailView>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    /**
     * GET /api/users?keyword=nam&include=departments&page=0&size=10&sort=createTime,desc
     * <p>
     * Departments are only joined and returned when requested through {@code include}.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<? extends UserSummaryView>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Set<String> include,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        boolean includeDepartments = include != null && include.contains(INCLUDE_DEPARTMENTS);

        Page<? extends UserSummaryView> users = userService.search(keyword, includeDepartments, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(users)));
    }
}
