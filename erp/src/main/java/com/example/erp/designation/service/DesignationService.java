package com.example.erp.designation.service;

import com.example.common.web.dto.PageResponse;
import com.example.erp.common.page.PageableSupport;
import com.example.erp.common.support.Entities;
import com.example.erp.common.support.Guards;
import com.example.erp.designation.dto.DesignationCreateRequest;
import com.example.erp.designation.dto.DesignationPatchRequest;
import com.example.erp.designation.dto.DesignationResponse;
import com.example.erp.designation.dto.DesignationUpdateRequest;
import com.example.erp.designation.entity.Designation;
import com.example.erp.designation.mapper.DesignationMapper;
import com.example.erp.designation.repository.DesignationDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** How this entity is named in a 404 or a duplicate-value message. */
    private static final String ENTITY = "Designation";

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS = PageableSupport.sortableFields("name");

    private final DesignationDao designationDao;

    public DesignationService(DesignationDao designationDao) {
        this.designationDao = designationDao;
    }

    // ------------------------------------------------------------------ read

    /** One page, optionally filtered by a case-insensitive name fragment. */
    public PageResponse<DesignationResponse> search(String keyword, Pageable pageable) {
        Page<Designation> page = designationDao.findByNameContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "", PageableSupport.sanitize(pageable, SORTABLE_FIELDS));
        return PageResponse.from(page, DesignationMapper::toResponse);
    }

    public DesignationResponse getById(Long id) {
        return DesignationMapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public DesignationResponse create(DesignationCreateRequest request) {
        Guards.assertNotTaken(designationDao.existsByNameIgnoreCase(request.name()),
                "name", "a designation", request.name());
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
        return Entities.findOrThrow(designationDao, id, ENTITY);
    }

    private void assertNameFree(String name, Long id) {
        Guards.assertNotTaken(designationDao.existsByNameIgnoreCaseAndIdNot(name, id),
                "name", "a designation", name);
    }
}
