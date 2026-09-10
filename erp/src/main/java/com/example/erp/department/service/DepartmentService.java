package com.example.erp.department.service;

import com.example.common.web.dto.PageResponse;
import com.example.common.web.exception.ResourceNotFoundException;
import com.example.erp.department.dto.DepartmentCreateRequest;
import com.example.erp.department.dto.DepartmentPatchRequest;
import com.example.erp.department.dto.DepartmentResponse;
import com.example.erp.department.dto.DepartmentUpdateRequest;
import com.example.erp.department.entity.Department;
import com.example.erp.department.mapper.DepartmentMapper;
import com.example.erp.department.repository.DepartmentDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * CRUD for departments.
 * <p>
 * Rules the schema cannot express live here: a uniqueness check runs before the insert so the
 * caller gets a 400 naming the field instead of a raw constraint violation, and a sort is
 * validated against an allowlist before it can reach the SQL.
 */
@Service
@Transactional(readOnly = true)
public class DepartmentService {

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "departmentCode", "createTime", "lastUpdateTime");

    /** Paging needs a deterministic order; fall back to the id when the caller gives none. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    private final DepartmentDao departmentDao;

    public DepartmentService(DepartmentDao departmentDao) {
        this.departmentDao = departmentDao;
    }

    // ------------------------------------------------------------------ read

    /** One page, optionally filtered by a case-insensitive name fragment. */
    public PageResponse<DepartmentResponse> search(String keyword, Pageable pageable) {
        Page<Department> page = departmentDao.findByNameContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "", withSafeSort(pageable));
        return PageResponse.from(page, DepartmentMapper::toResponse);
    }

    public DepartmentResponse getById(Long id) {
        return DepartmentMapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        if (departmentDao.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                    "name: a department with name '" + request.name() + "' already exists");
        }
        if (departmentDao.existsByDepartmentCodeIgnoreCase(request.departmentCode())) {
            throw new IllegalArgumentException(
                    "departmentCode: a department with code '" + request.departmentCode() + "' already exists");
        }
        Department saved = departmentDao.save(DepartmentMapper.toEntity(request));
        return DepartmentMapper.toResponse(saved);
    }

    /** Full replacement. Fields the caller left out are written as null. */
    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
        Department department = findOrThrow(id);
        assertNameFree(request.name(), id);
        assertCodeFree(request.departmentCode(), id);
        DepartmentMapper.replace(department, request);
        return DepartmentMapper.toResponse(department);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public DepartmentResponse patch(Long id, DepartmentPatchRequest request) {
        Department department = findOrThrow(id);
        if (request.name() != null) {
            assertNameFree(request.name(), id);
        }
        if (request.departmentCode() != null) {
            assertCodeFree(request.departmentCode(), id);
        }
        DepartmentMapper.merge(department, request);
        return DepartmentMapper.toResponse(department);
    }

    @Transactional
    public void delete(Long id) {
        departmentDao.delete(findOrThrow(id));
    }

    // ---------------------------------------------------------------- helper

    private Department findOrThrow(Long id) {
        return departmentDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    private void assertNameFree(String name, Long id) {
        if (departmentDao.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                    "name: a department with name '" + name + "' already exists");
        }
    }

    private void assertCodeFree(String departmentCode, Long id) {
        if (departmentDao.existsByDepartmentCodeIgnoreCaseAndIdNot(departmentCode, id)) {
            throw new IllegalArgumentException(
                    "departmentCode: a department with code '" + departmentCode + "' already exists");
        }
    }

    /**
     * Rejects a sort on a property that is not in {@link #SORTABLE_FIELDS}, and supplies a
     * deterministic order when the request carries none.
     */
    private Pageable withSafeSort(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }
        sort.forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("sort: unsupported property '" + order.getProperty()
                        + "', allowed: " + SORTABLE_FIELDS);
            }
        });
        return pageable;
    }
}
