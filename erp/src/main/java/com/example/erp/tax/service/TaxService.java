package com.example.erp.tax.service;

import com.example.common.web.dto.PageResponse;
import com.example.erp.common.page.PageableSupport;
import com.example.erp.common.support.Entities;
import com.example.erp.common.support.Guards;
import com.example.erp.tax.dto.TaxCreateRequest;
import com.example.erp.tax.dto.TaxPatchRequest;
import com.example.erp.tax.dto.TaxResponse;
import com.example.erp.tax.dto.TaxSearchRequest;
import com.example.erp.tax.dto.TaxUpdateRequest;
import com.example.erp.tax.entity.Tax;
import com.example.erp.tax.mapper.TaxMapper;
import com.example.erp.tax.repository.TaxDao;
import com.example.erp.tax.repository.TaxSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * CRUD for taxes.
 * <p>
 * Rules the schema cannot express live here: a uniqueness check runs before the insert so the
 * caller gets a 400 naming the field instead of a raw constraint violation, and a sort is
 * validated against an allowlist before it can reach the SQL.
 */
@Service
@Transactional(readOnly = true)
public class TaxService {

    /** How this entity is named in a 404 or a duplicate-value message. */
    private static final String ENTITY = "Tax";

    /** Properties a client may sort by; anything else is rejected instead of reaching the SQL. */
    private static final Set<String> SORTABLE_FIELDS =
            PageableSupport.sortableFields("name", "displayName", "type", "percentage", "startDate", "endDate");

    private final TaxDao taxDao;

    public TaxService(TaxDao taxDao) {
        this.taxDao = taxDao;
    }

    // ------------------------------------------------------------------ read

    /** One page, narrowed by whichever filters the request carried. */
    public PageResponse<TaxResponse> search(TaxSearchRequest request, Pageable pageable) {
        Page<Tax> page = taxDao.findAll(TaxSpecifications.from(request),
                PageableSupport.sanitize(pageable, SORTABLE_FIELDS));
        return PageResponse.from(page, TaxMapper::toResponse);
    }

    public TaxResponse getById(Long id) {
        return TaxMapper.toResponse(findOrThrow(id));
    }

    // ----------------------------------------------------------------- write

    @Transactional
    public TaxResponse create(TaxCreateRequest request) {
        Guards.assertNotTaken(taxDao.existsByNameIgnoreCase(request.name()), "name", "a tax", request.name());
        Tax saved = taxDao.save(TaxMapper.toEntity(request));
        return TaxMapper.toResponse(saved);
    }

    /** Full replacement. Fields the caller left out are written as null. */
    @Transactional
    public TaxResponse update(Long id, TaxUpdateRequest request) {
        Tax tax = findOrThrow(id);
        assertNameFree(request.name(), id);
        TaxMapper.replace(tax, request);
        return TaxMapper.toResponse(tax);
    }

    /** Partial update: only the fields present in the payload are touched. */
    @Transactional
    public TaxResponse patch(Long id, TaxPatchRequest request) {
        Tax tax = findOrThrow(id);
        if (request.name() != null) {
            assertNameFree(request.name(), id);
        }
        TaxMapper.merge(tax, request);
        return TaxMapper.toResponse(tax);
    }

    @Transactional
    public void delete(Long id) {
        taxDao.delete(findOrThrow(id));
    }

    // ---------------------------------------------------------------- helper

    private Tax findOrThrow(Long id) {
        return Entities.findOrThrow(taxDao, id, ENTITY);
    }

    private void assertNameFree(String name, Long id) {
        Guards.assertNotTaken(taxDao.existsByNameIgnoreCaseAndIdNot(name, id), "name", "a tax", name);
    }
}
