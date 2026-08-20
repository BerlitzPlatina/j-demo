package com.example.orm.jpa.service;

import com.example.orm.jpa.dto.DepartmentResponse;
import com.example.orm.jpa.dto.PageResponse;
import com.example.orm.jpa.dto.UserCreateRequest;
import com.example.orm.jpa.dto.UserDepartmentLink;
import com.example.orm.jpa.dto.UserInclude;
import com.example.orm.jpa.dto.UserResponse;
import com.example.orm.jpa.dto.UserUpdateRequest;
import com.example.orm.jpa.entity.Department;
import com.example.orm.jpa.entity.User;
import com.example.orm.jpa.exception.ResourceNotFoundException;
import com.example.orm.jpa.mapper.UserMapper;
import com.example.orm.jpa.repository.DepartmentDao;
import com.example.orm.jpa.repository.UserDao;
import com.example.orm.jpa.repository.UserDepartmentDao;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CRUD for users.
 * <p>
 * What gets loaded is decided by the caller through {@link UserInclude}, not by the mapping:
 * every relation is {@code LAZY}, so nothing is fetched unless it was asked for, and when it is
 * asked for it is fetched in its own query for the whole page.
 * <p>
 * The read side is written so the query count never grows with the page size:
 * <ul>
 *   <li>list without departments: 1 page query + 1 count query</li>
 *   <li>list with departments: 1 page query + 1 count query + 1 join-table query + 1 department
 *       query, matched together in memory (eager loading, not a fetch join)</li>
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
    private final UserDepartmentDao userDepartmentDao;
    private final DepartmentDao departmentDao;

    public UserService(UserDao userDao, UserDepartmentDao userDepartmentDao, DepartmentDao departmentDao) {
        this.userDao = userDao;
        this.userDepartmentDao = userDepartmentDao;
        this.departmentDao = departmentDao;
    }

    // ------------------------------------------------------------------ read

    /**
     * Returns a page of users, optionally filtered by a case-insensitive name fragment.
     * <p>
     * {@code includes} is the fetch plan: a relation is loaded only when it is in the set, and
     * then in its own batched queries for the whole page - never per row.
     */
    public PageResponse<UserResponse> search(String keyword, Set<UserInclude> includes, Pageable pageable) {
        Page<User> page = userDao.findByNameContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "", withSafeSort(pageable));

        if (!includes.contains(UserInclude.DEPARTMENTS)) {
            return PageResponse.from(page, UserMapper::toSummary);
        }
        return PageResponse.of(page, withDepartments(page.getContent()));
    }

    /**
     * Returns a single user, with the relations named in {@code includes} and nothing else.
     * A single row can afford a fetch join, so this stays one query either way.
     */
    public UserResponse getById(Long id, Set<UserInclude> includes) {
        if (includes.contains(UserInclude.DEPARTMENTS)) {
            return UserMapper.toDetail(findDetailOrThrow(id));
        }
        return UserMapper.toSummary(userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id)));
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
     * Rest of the eager load, in two more queries and a bit of in-memory matching:
     * <ol>
     *   <li>select the join table rows of the page's user ids - pairs of (userId, departmentId)</li>
     *   <li>select those department ids from {@code orm_department}</li>
     *   <li>index the departments by id, then walk the links to give every user its own list</li>
     * </ol>
     * The page's order is preserved, and a user with no link gets an empty list.
     */
    private List<UserResponse> withDepartments(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(User::getId).toList();

        // Query 1 of the relation: the links, ids only.
        List<UserDepartmentLink> links = userDepartmentDao.findLinksByUserIdIn(userIds);

        // Query 2 of the relation: the departments those links point at, skipped when there are none.
        Set<Long> departmentIds = links.stream().map(UserDepartmentLink::departmentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DepartmentResponse> departmentsById = departmentIds.isEmpty()
                ? Map.of()
                : departmentDao.findResponsesByIdIn(departmentIds).stream()
                        .collect(Collectors.toMap(DepartmentResponse::id, Function.identity()));

        // Matching: link.departmentId -> the department loaded above.
        Map<Long, List<DepartmentResponse>> byUserId = new HashMap<>();
        for (UserDepartmentLink link : links) {
            DepartmentResponse department = departmentsById.get(link.departmentId());
            if (department != null) {
                byUserId.computeIfAbsent(link.userId(), key -> new ArrayList<>()).add(department);
            }
        }

        List<UserResponse> content = new ArrayList<>(users.size());
        for (User user : users) {
            content.add(UserMapper.toDetail(user, byUserId.getOrDefault(user.getId(), List.of())));
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
