package com.example.erp.contact.controller;

import com.example.common.web.dto.ApiResponse;
import com.example.common.web.dto.PageResponse;
import com.example.erp.contact.dto.ContactCreateRequest;
import com.example.erp.contact.dto.ContactResponse;
import com.example.erp.contact.dto.ContactSearchRequest;
import com.example.erp.contact.service.ContactService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contact read API. Answers with the {@link ApiResponse} envelope and
 * {@link ContactResponse}
 * transfer objects; entities never leave the service layer.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * GET /api/contacts?keyword=acme&contactType=customer&status=ACTIVE
     * &creditLimitFrom=1000&page=0&size=10&sort=contactName,asc
     * <p>
     * The filters arrive as one {@code @ModelAttribute} rather than a dozen
     * {@code @RequestParam}s, so adding a filter is a field on
     * {@link ContactSearchRequest} and
     * a line in {@code ContactSpecifications}, not a change to this signature. An
     * absent
     * parameter stays null and is dropped from the query altogether.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContactResponse>>> getContacts(
            @ModelAttribute ContactSearchRequest request,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(contactService.search(request, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> createContact(
            @RequestParam(required = true) Long organizationId,
            @Valid @RequestBody ContactCreateRequest request) {
        ContactResponse created = contactService.create(request, organizationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> getContact(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contactService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> update(@PathVariable Long id,
            @Valid @RequestBody ContactCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(contactService.update(request, id)));
    }
}
