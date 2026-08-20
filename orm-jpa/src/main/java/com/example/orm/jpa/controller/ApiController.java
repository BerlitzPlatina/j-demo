package com.example.orm.jpa.controller;

import com.example.orm.jpa.dto.ApiResponse;
import com.example.orm.jpa.dto.PageResponse;
import com.example.orm.jpa.dto.UserCreateRequest;
import com.example.orm.jpa.dto.UserInclude;
import com.example.orm.jpa.dto.UserResponse;
import com.example.orm.jpa.dto.UserUpdateRequest;
import com.example.orm.jpa.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * User CRUD API. Every method answers with the {@link ApiResponse} envelope and
 * {@link UserResponse} transfer objects; entities never leave the service
 * layer.
 */
@RestController
@RequestMapping("/api/users")
public class ApiController {

    private final UserService userService;

    public ApiController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users?keyword=nam&include=departments&page=0&size=10&sort=createTime,desc
     * <p>
     * {@code include} is the fetch plan, checked against {@link UserInclude}: a relation is
     * loaded only when named, an unknown name is a 400. Requesting {@code departments} costs one
     * extra join-table query plus one department query for the whole page, never one per row.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Set<String> include,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                userService.search(keyword, UserInclude.parse(include), pageable)));
    }

    /**
     * GET /api/users/{id}?include=departments
     * <p>
     * Same fetch plan as the list endpoint: without {@code include} the response carries no
     * relation at all.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id,
            @RequestParam(required = false) Set<String> include) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id, UserInclude.parse(include))));
    }

    /**
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    /**
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.update(id, request)));
    }

    /**
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Deleted"));
    }
}
