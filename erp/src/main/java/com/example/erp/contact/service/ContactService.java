package com.example.erp.contact.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.web.dto.PageResponse;
import com.example.erp.contact.dto.ContactCreateRequest;
import com.example.erp.contact.dto.ContactResponse;
import com.example.erp.contact.dto.ContactSearchRequest;
import com.example.erp.contact.entity.Contact;
import com.example.erp.contact.mapper.ContactMapper;
import com.example.erp.contact.repository.ContactDao;
import com.example.erp.contact.repository.ContactSpecifications;

@Service
@Transactional(readOnly = true)
public class ContactService {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id");
    /**
     * JPA property names a client may sort by; anything else is rejected instead of
     * reaching the SQL.
     */
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "contactNumber", "contactName", "companyName", "contactType", "status",
            "creditLimit", "outstandingReceivableAmount", "createTime", "lastUpdateTime");

    private final ContactDao contactDao;

    public ContactService(ContactDao contactDao) {
        this.contactDao = contactDao;
    }

    public PageResponse<ContactResponse> search(ContactSearchRequest request, Pageable pageable) {
        Page<Contact> page = contactDao.findAll(ContactSpecifications.from(request), withSafeSort(pageable));
        return PageResponse.from(page, ContactMapper::toResponse);
    }

    @Transactional
    public ContactResponse create(ContactCreateRequest request, Long organizationId) {
        Contact saved = contactDao.save(ContactMapper.toEntity(request, organizationId));
        return ContactMapper.toResponse(saved);
    }

    private Pageable withSafeSort(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("sort: unsupported property '" + order.getProperty()
                        + "', allowed: " + SORTABLE_FIELDS);
            }
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
