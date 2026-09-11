package com.example.erp.contact.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.web.dto.PageResponse;
import com.example.erp.common.page.PageableSupport;
import com.example.erp.common.support.Entities;
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
    /** How this entity is named in a 404 or a duplicate-value message. */
    private static final String ENTITY = "Contact";

    /**
     * JPA property names a client may sort by; anything else is rejected instead of
     * reaching the SQL.
     */
    private static final Set<String> SORTABLE_FIELDS = PageableSupport.sortableFields(
            "contactNumber", "contactName", "companyName", "contactType", "status",
            "creditLimit", "outstandingReceivableAmount", "organization.name");

    private final ContactDao contactDao;

    public ContactService(ContactDao contactDao) {
        this.contactDao = contactDao;
    }

    public PageResponse<ContactResponse> search(ContactSearchRequest request, Pageable pageable) {
        Page<Contact> page = contactDao.findAll(ContactSpecifications.from(request),
                PageableSupport.sanitize(pageable, SORTABLE_FIELDS));
        return PageResponse.from(page, ContactMapper::toResponse);
    }

    @Transactional
    public ContactResponse create(ContactCreateRequest request, Long organizationId) {
        Contact saved = contactDao.save(ContactMapper.toEntity(request, organizationId));
        return ContactMapper.toResponse(saved);
    }

    public ContactResponse getById(Long id) {
        return ContactMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ContactResponse update(ContactCreateRequest request, Long contactId) {
        Contact contact = findOrThrow(contactId);
        if (request.contactName() != null) {
            contact.setContactName(request.contactName());
        }
        return ContactMapper.toResponse(contact);
    }

    private Contact findOrThrow(Long id) {
        return Entities.findOrThrow(contactDao, id, ENTITY);
    }
}
