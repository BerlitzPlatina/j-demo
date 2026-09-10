package com.example.erp.designation.service;

import com.example.common.web.dto.PageResponse;
import com.example.common.web.exception.ResourceNotFoundException;
import com.example.erp.designation.dto.DesignationCreateRequest;
import com.example.erp.designation.dto.DesignationPatchRequest;
import com.example.erp.designation.dto.DesignationResponse;
import com.example.erp.designation.dto.DesignationUpdateRequest;
import com.example.erp.designation.entity.Designation;
import com.example.erp.designation.mapper.DesignationMapper;
import com.example.erp.designation.repository.DesignationDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * CRUD for designations.
 * <p>
 * Rules the schema cannot express live here: a uniqueness check runs before the insert so the
 * caller gets a 400 naming the field instead of a raw constraint violation, and a sort is
 * validated against an allowlist before it can reach the SQL.
 */
@Service
@Transactional(readOnly = true)
public class DesignationService {

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "createTime", "lastUpdateTime");

    /** Paging needs a deterministic order; fall back to the id when the caller gives none. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    private final DesignationDao designationDao;

    public DesignationService(DesignationDao designationDao) {
        this.designationDao = designationDao;
    }

    // ------------------------------------------------------------------ read

    /** One page, optionally filtered by a case-insensitive name fragment. */
    public PageResponse<DesignationResponse> search(String keyword, Pageable pageable) {
        Page<Designation> page = designationDao.findByNameContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "", withSafeSort(pageable));
        return PageResponse.from(page, DesignationMapper::toResponse);
    }

    public DesignationResponse getById(Long id) {
        return DesignationMapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public DesignationResponse create(DesignationCreateRequest request) {
        if (designationDao.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                    "name: a designation with name '" + request.name() + "' already exists");
        }
        Designation saved = designationDao.save(DesignationMapper.toEntity(request));
        return DesignationMapper.toResponse(saved);
    }

    /** Full replacement. Fields the caller left out are written as null. */
    @Transactional
    public DesignationResponse update(Long id, DesignationUpdateRequest request) {
        Designation designation = findOrThrow(id);
        assertNameFree(request.name(), id);
        DesignationMapper.replace(designation, request);
        return DesignationMapper.toResponse(designation);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public DesignationResponse patch(Long id, DesignationPatchRequest request) {
        Designation designation = findOrThrow(id);
        if (request.name() != null) {
            assertNameFree(request.name(), id);
        }
        DesignationMapper.merge(designation, request);
        return DesignationMapper.toResponse(designation);
    }

    @Transactional
    public void delete(Long id) {
        designationDao.delete(findOrThrow(id));
    }

    // ---------------------------------------------------------------- helper

    private Designation findOrThrow(Long id) {
        return designationDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
    }

    private void assertNameFree(String name, Long id) {
        if (designationDao.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                    "name: a designation with name '" + name + "' already exists");
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
