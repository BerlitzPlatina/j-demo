package com.example.orm.jpa.service;

import com.example.orm.jpa.dto.PageResponse;
import com.example.orm.jpa.dto.UserCreateRequest;
import com.example.orm.jpa.dto.UserResponse;
import com.example.orm.jpa.dto.UserUpdateRequest;
import com.example.orm.jpa.entity.Department;
import com.example.orm.jpa.entity.User;
import com.example.orm.jpa.exception.ResourceNotFoundException;
import com.example.orm.jpa.mapper.UserMapper;
import com.example.orm.jpa.repository.DepartmentDao;
import com.example.orm.jpa.repository.UserDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CRUD for users.
 * <p>
 * The read side is written so the query count never grows with the page size:
 * <ul>
 *   <li>list without departments: 1 page query + 1 count query</li>
 *   <li>list with departments: 1 page query + 1 count query + 1 collection query for the page ids</li>
 *   <li>detail: 1 fetch-join query</li>
 * </ul>
 * That is what keeps N+1 away — {@code departmentList} is mapped {@code LAZY}, and nothing
 * touches it outside of a query that explicitly fetched it.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "email", "status", "createTime", "lastUpdateTime", "lastLoginTime");

    /** Paging needs a deterministic order; fall back to the id when the caller gives none. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    private static final int DEFAULT_STATUS = 1;

    private final UserDao userDao;
    private final DepartmentDao departmentDao;

    public UserService(UserDao userDao, DepartmentDao departmentDao) {
        this.userDao = userDao;
        this.departmentDao = departmentDao;
    }

    // ------------------------------------------------------------------ read

    /**
     * Returns a page of users, optionally filtered by a case-insensitive name fragment.
     * Departments are loaded only when requested, and then in one extra query for the whole page.
     */
    public PageResponse<UserResponse> search(String keyword, boolean includeDepartments, Pageable pageable) {
        Page<User> page = userDao.findByNameContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "", withSafeSort(pageable));

        if (!includeDepartments) {
            return PageResponse.from(page, UserMapper::toSummary);
        }
        return PageResponse.of(page, withDepartments(page.getContent()));
    }

    /**
     * Returns a single user with departments.
     */
    public UserResponse getById(Long id) {
        return UserMapper.toDetail(findDetailOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String salt = UUID.randomUUID().toString().replace("-", "");
        User user = User.builder()
                .name(request.name())
                .password(encrypt(request.password(), salt))
                .salt(salt)
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .status(request.status() == null ? DEFAULT_STATUS : request.status())
                .departmentList(resolveDepartments(request.departmentIds()))
                .build();

        return UserMapper.toDetail(userDao.saveAndFlush(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findDetailOrThrow(id);
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        // null means "leave the assignment as it is"; an empty list clears it.
        if (request.departmentIds() != null) {
            user.setDepartmentList(resolveDepartments(request.departmentIds()));
        }

        return UserMapper.toDetail(userDao.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        // Removing the user only drops its rows in orm_user_dept; the departments themselves stay.
        userDao.delete(user);
    }

    // --------------------------------------------------------------- helpers

    private User findDetailOrThrow(Long id) {
        return userDao.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Maps the page content to detail responses, loading every department of the page in one query
     * and keeping the page's original order.
     */
    private List<UserResponse> withDepartments(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> ids = users.stream().map(User::getId).toList();
        Map<Long, User> fetched = userDao.findAllWithDepartmentsByIdIn(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, second) -> first));

        List<UserResponse> content = new ArrayList<>(users.size());
        for (Long id : ids) {
            User fetchedUser = fetched.get(id);
            if (fetchedUser != null) {
                content.add(UserMapper.toDetail(fetchedUser));
            }
        }
        return content;
    }

    private Collection<Department> resolveDepartments(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> distinctIds = List.copyOf(new HashSet<>(departmentIds));
        List<Department> departments = departmentDao.findAllById(distinctIds);
        if (departments.size() != distinctIds.size()) {
            Set<Long> found = departments.stream().map(Department::getId).collect(Collectors.toSet());
            List<Long> missing = distinctIds.stream().filter(id -> !found.contains(id)).sorted().toList();
            throw new ResourceNotFoundException("Department not found with id: " + missing);
        }
        return new ArrayList<>(departments);
    }

    private Pageable withSafeSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort property: " + order.getProperty() + ". Allowed: " + SORTABLE_FIELDS);
            }
        });
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
    }

    /**
     * Demo-only password hashing (MD5 + salt), matching the 32-char column and the seeded rows.
     * Real applications should use BCrypt/Argon2.
     */
    private static String encrypt(String rawPassword, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest((rawPassword + salt).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
