package com.example.erp.legalentity.service;

import com.example.common.web.dto.PageResponse;
import com.example.erp.common.page.PageableSupport;
import com.example.erp.common.support.Entities;
import com.example.erp.common.support.Guards;
import com.example.erp.legalentity.dto.LegalEntityCreateRequest;
import com.example.erp.legalentity.dto.LegalEntityPatchRequest;
import com.example.erp.legalentity.dto.LegalEntityResponse;
import com.example.erp.legalentity.dto.LegalEntityUpdateRequest;
import com.example.erp.legalentity.entity.LegalEntity;
import com.example.erp.legalentity.mapper.LegalEntityMapper;
import com.example.erp.legalentity.repository.LegalEntityDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * CRUD for legal entities.
 * <p>
 * Rules the schema cannot express live here: a uniqueness check runs before the insert so the
 * caller gets a 400 naming the field instead of a raw constraint violation, and a sort is
 * validated against an allowlist before it can reach the SQL.
 */
@Service
@Transactional(readOnly = true)
public class LegalEntityService {

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS = PageableSupport.sortableFields("entityCode", "registeredName", "registrationNumber", "taxRegistrationNumber", "countryCode", "baseCurrency", "entityStatus", "legacyDefault", "source", "createdBy", "updatedBy");

    /** How this entity is named in a 404 or a duplicate-value message. */
    private static final String ENTITY = "LegalEntity";

    private final LegalEntityDao legalEntityDao;

    public LegalEntityService(LegalEntityDao legalEntityDao) {
        this.legalEntityDao = legalEntityDao;
    }

    // ------------------------------------------------------------------ read

    /** One page, optionally filtered by a case-insensitive entityCode fragment. */
    public PageResponse<LegalEntityResponse> search(String keyword, Pageable pageable) {
        Page<LegalEntity> page = legalEntityDao.findByEntityCodeContainingIgnoreCase(
                StringUtils.hasText(keyword) ? keyword.trim() : "",
                PageableSupport.sanitize(pageable, SORTABLE_FIELDS));
        return PageResponse.from(page, LegalEntityMapper::toResponse);
    }

    public LegalEntityResponse getById(Long id) {
        return LegalEntityMapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public LegalEntityResponse create(LegalEntityCreateRequest request) {
        Guards.assertNotTaken(legalEntityDao.existsByEntityCodeIgnoreCase(request.entityCode()),
                "entityCode", "a legalEntity", request.entityCode());
        Guards.assertNotTaken(legalEntityDao.existsByLegacyDefault(request.legacyDefault()),
                "legacyDefault", "a legalEntity", request.legacyDefault());
        LegalEntity saved = legalEntityDao.save(LegalEntityMapper.toEntity(request));
        return LegalEntityMapper.toResponse(saved);
    }

    /** Full replacement. Fields the caller left out are written as null. */
    @Transactional
    public LegalEntityResponse update(Long id, LegalEntityUpdateRequest request) {
        LegalEntity legalEntity = findOrThrow(id);
        assertEntityCodeFree(request.entityCode(), id);
        assertLegacyDefaultFree(request.legacyDefault(), id);
        LegalEntityMapper.replace(legalEntity, request);
        return LegalEntityMapper.toResponse(legalEntity);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public LegalEntityResponse patch(Long id, LegalEntityPatchRequest request) {
        LegalEntity legalEntity = findOrThrow(id);
        if (request.entityCode() != null) {
            assertEntityCodeFree(request.entityCode(), id);
        }
        if (request.legacyDefault() != null) {
            assertLegacyDefaultFree(request.legacyDefault(), id);
        }
        LegalEntityMapper.merge(legalEntity, request);
        return LegalEntityMapper.toResponse(legalEntity);
    }

    @Transactional
    public void delete(Long id) {
        legalEntityDao.delete(findOrThrow(id));
    }

    // ---------------------------------------------------------------- helper

    private LegalEntity findOrThrow(Long id) {
        return Entities.findOrThrow(legalEntityDao, id, ENTITY);
    }

    private void assertEntityCodeFree(String entityCode, Long id) {
        Guards.assertNotTaken(legalEntityDao.existsByEntityCodeIgnoreCaseAndIdNot(entityCode, id),
                "entityCode", "a legalEntity", entityCode);
    }

    private void assertLegacyDefaultFree(Boolean legacyDefault, Long id) {
        Guards.assertNotTaken(legalEntityDao.existsByLegacyDefaultAndIdNot(legacyDefault, id),
                "legacyDefault", "a legalEntity", legacyDefault);
    }
}
