package com.example.erp.contact.repository;

import org.springframework.data.jpa.domain.Specification;

import com.example.erp.common.constant.ErpConstants;
import com.example.erp.common.specification.SpecBuilder;
import com.example.erp.common.specification.Specs;
import com.example.erp.contact.dto.ContactSearchRequest;
import com.example.erp.contact.entity.Contact;
import com.example.erp.organization.entity.Organization;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * The {@code where} of the contact search.
 * <p>
 * Only the two conditions that reach the organizations table are written out here; everything
 * else is an ordinary optional filter, which {@link SpecBuilder} already knows how to drop
 * when the request leaves it out.
 */
public final class ContactSpecifications {

    private ContactSpecifications() {
    }

    public static Specification<Contact> from(ContactSearchRequest request) {
        return SpecBuilder.<Contact>of()
                .eq("contactType", request.contactType())
                .like("contactName", request.contactName())
                .like("companyName", request.companyName())
                .eq("status", request.status())
                .eq("organizationId", request.organizationId())
                .and(organizationName(request.organizationName()))
                .eq("customerSubType", request.customerSubType())
                .eq("hasTransaction", request.hasTransaction())
                .and(keyword(request.keyword()))
                .between("creditLimit", request.creditLimitFrom(), request.creditLimitTo())
                .dayRange(ErpConstants.CREATE_TIME, request.createdFrom(), request.createdTo())
                .build();
    }

    /** Filters on the name of the organization the contact belongs to. */
    private static Specification<Contact> organizationName(String keyword) {
        String pattern = Specs.pattern(keyword);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) -> organizationNameExists(root, query, cb, pattern);
    }

    /**
     * One search box, spread over the columns a user actually types into - including the
     * organization name, which is why this cannot be a plain
     * {@link SpecBuilder#likeAny(String, String...)}.
     */
    private static Specification<Contact> keyword(String keyword) {
        String pattern = Specs.pattern(keyword);
        if (pattern == null) {
            return null;
        }
        return (root, query, cb) -> cb.or(
                Specs.likeLower(cb, root.get("contactName"), pattern),
                Specs.likeLower(cb, root.get("companyName"), pattern),
                Specs.likeLower(cb, root.get("legalName"), pattern),
                Specs.likeLower(cb, root.get("contactNumber"), pattern),
                organizationNameExists(root, query, cb, pattern));
    }

    /**
     * <pre>
     * exists (select 1 from organizations o
     *         where o.id = c.organization_id and lower(o.name) like ?)
     * </pre>
     * <p>
     * Correlated on the plain {@code organizationId} column, so a contact with no organization
     * simply fails the condition instead of needing an outer join.
     */
    private static Predicate organizationNameExists(Root<Contact> root, CriteriaQuery<?> query,
            CriteriaBuilder cb, String pattern) {
        return Specs.exists(query, cb, Organization.class, (organization, builder) -> builder.and(
                builder.equal(organization.get(ErpConstants.ID), root.get("organizationId")),
                Specs.likeLower(builder, organization.get("name"), pattern)));
    }
}
