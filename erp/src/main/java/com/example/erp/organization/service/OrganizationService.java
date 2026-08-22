package com.example.erp.organization.service;

import com.example.common.web.dto.PageResponse;
import com.example.common.web.exception.ResourceNotFoundException;
import com.example.erp.organization.dto.OrganizationCreateRequest;
import com.example.erp.organization.dto.OrganizationPatchRequest;
import com.example.erp.organization.dto.OrganizationResponse;
import com.example.erp.organization.dto.OrganizationUpdateRequest;
import com.example.erp.organization.entity.Organization;
import com.example.erp.organization.mapper.OrganizationMapper;
import com.example.erp.organization.repository.OrganizationDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * CRUD for organizations.
 * <p>
 * Two rules live here rather than in the schema, because the schema cannot express either:
 * <ul>
 *   <li>the name is unique case-insensitively - checked before the insert so the caller gets a
 *   400 naming the field instead of a raw constraint violation;</li>
 *   <li>at most one organization is the default one - setting the flag moves it off whichever row
 *   held it, in one UPDATE.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class OrganizationService {

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "userStatus", "industryType", "accountCreatedDate", "createTime", "lastUpdateTime");

    /** Paging needs a deterministic order; fall back to the id when the caller gives none. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");

    private final OrganizationDao organizationDao;

    public OrganizationService(OrganizationDao organizationDao) {
        this.organizationDao = organizationDao;
    }

    // ------------------------------------------------------------------ read

    /** One page of organizations, optionally filtered by a case-insensitive name fragment. */
    public PageResponse<OrganizationResponse> search(String keyword, Pageable pageable) {
        Page<Organization> page = organizationDao.findByNameContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "", withSafeSort(pageable));
        return PageResponse.from(page, OrganizationMapper::toResponse);
    }

    public OrganizationResponse getById(Long id) {
        return OrganizationMapper.toResponse(findOrThrow(id));
    }

    /** The tenant a caller falls back to when it was given no organization at all. */
    public OrganizationResponse getDefault() {
        return organizationDao.findFirstByDefaultOrgIsTrue()
                .map(OrganizationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No default organization is configured"));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public OrganizationResponse create(OrganizationCreateRequest request) {
        if (organizationDao.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("name: an organization named '" + request.name() + "' already exists");
        }
        Organization saved = organizationDao.save(OrganizationMapper.toEntity(request));
        applyDefaultFlag(saved);
        return OrganizationMapper.toResponse(saved);
    }

    /** Full replacement. Fields the caller left out are written as null. */
    @Transactional
    public OrganizationResponse update(Long id, OrganizationUpdateRequest request) {
        Organization organization = findOrThrow(id);
        assertNameFree(request.name(), id);
        OrganizationMapper.replace(organization, request);
        applyDefaultFlag(organization);
        return OrganizationMapper.toResponse(organization);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public OrganizationResponse patch(Long id, OrganizationPatchRequest request) {
        Organization organization = findOrThrow(id);
        if (request.name() != null) {
            assertNameFree(request.name(), id);
        }
        OrganizationMapper.merge(organization, request);
        applyDefaultFlag(organization);
        return OrganizationMapper.toResponse(organization);
    }

    /**
     * Hard delete. Rows in {@code addresses} point here through a foreign key, so deleting an
     * organization that still has addresses fails with a 409 rather than orphaning them.
     */
    @Transactional
    public void delete(Long id) {
        Organization organization = findOrThrow(id);
        if (Boolean.TRUE.equals(organization.getDefaultOrg())) {
            throw new IllegalArgumentException(
                    "the default organization cannot be deleted; make another one the default first");
        }
        organizationDao.delete(organization);
    }

    // ---------------------------------------------------------------- helper

    private Organization findOrThrow(Long id) {
        return organizationDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
    }

    private void assertNameFree(String name, Long id) {
        if (organizationDao.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("name: an organization named '" + name + "' already exists");
        }
    }

    /** Keeps the default flag on exactly one row: whichever one just claimed it. */
    private void applyDefaultFlag(Organization organization) {
        if (Boolean.TRUE.equals(organization.getDefaultOrg())) {
            organizationDao.clearDefaultExcept(organization.getId());
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
