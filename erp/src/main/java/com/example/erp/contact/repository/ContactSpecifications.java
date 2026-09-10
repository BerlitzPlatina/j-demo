package com.example.erp.contact.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.example.erp.contact.dto.ContactSearchRequest;
import com.example.erp.contact.entity.Contact;
import com.example.erp.organization.entity.Organization;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public final class ContactSpecifications {

    private ContactSpecifications() {
    }

    /**
     * Combines the conditions that carry a value; a null filter is dropped
     * outright, so it
     * never reaches the SQL.
     */
    public static Specification<Contact> from(ContactSearchRequest request) {
        // The nulls have to be stripped before allOf sees them: it rejects a null
        // element with
        // "Other specification must not be null" rather than skipping it, so a request
        // with any
        // filter left out would answer 400.
        List<Specification<Contact>> conditions = Stream.<Specification<Contact>>of(
                eq("contactType", request.contactType()),
                like("contactName", request.contactName()),
                like("companyName", request.companyName()),
                eq("status", request.status()),
                eq("organizationId", request.organizationId()),
                organizationName(request.organizationName()),
                eq("customerSubType", request.customerSubType()),
                eq("hasTransaction", request.hasTransaction()),
                keyword(request.keyword()),
                between("creditLimit", request.creditLimitFrom(), request.creditLimitTo()),
                createdBetween(request.createdFrom(), request.createdTo()))
                .filter(Objects::nonNull)
                .toList();
        log.debug("conditions count: {}", conditions.size());
        return conditions.isEmpty() ? Specification.unrestricted() : Specification.allOf(conditions);
    }

    private static <T> Specification<Contact> eq(String attribute, T value) {
        if (value == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute), value);
    }

    private static <T> Specification<Contact> like(String attribute, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(attribute)), like);
    }

    /**
     * Filters on the name of the organization the contact belongs to, as an
     * {@code exists} subquery rather than a join: the contact row is the only thing
     * the outer query selects, so nothing depends on the organization being in the
     * from-clause, and the subquery cannot multiply or drop rows no matter how the
     * rest of the specification is composed.
     */
    private static Specification<Contact> organizationName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> organizationNameExists(root, query, cb, like);
    }

    /** One search box, spread over the columns a user actually types into. */
    private static Specification<Contact> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("contactName")), like),
                cb.like(cb.lower(root.get("companyName")), like),
                cb.like(cb.lower(root.get("legalName")), like),
                cb.like(cb.lower(root.get("contactNumber")), like),
                organizationNameExists(root, query, cb, like));
    }

    /**
     * <pre>
     * exists (select 1 from organizations o
     *         where o.id = c.organization_id and lower(o.name) like ?)
     * </pre>
     * <p>
     * Correlated on the plain {@code organizationId} column, so a contact with no
     * organization simply fails the condition instead of needing an outer join.
     */
    private static Predicate organizationNameExists(Root<Contact> root, CriteriaQuery<?> query,
            CriteriaBuilder cb, String like) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<Organization> organization = subquery.from(Organization.class);
        return cb.exists(subquery.select(cb.literal(1))
                .where(cb.equal(organization.get("id"), root.get("organizationId")),
                        cb.like(cb.lower(organization.get("name")), like)));
    }

    /**
     * Filters on the audit timestamp. That column is {@code create_time} in the
     * table but
     * {@code createTime} on the entity - and it is a
     * {@code DATETIME}, so an
     * inclusive "to" has to reach the start of the following day rather than 00:00
     * of its own.
     */
    private static Specification<Contact> createdBetween(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        Date fromInclusive = from == null ? null : toDate(from);
        Date toExclusive = to == null ? null : toDate(to.plusDays(1));
        return (root, query, cb) -> {
            Path<Date> createTime = root.get("createTime");
            if (fromInclusive == null) {
                return cb.lessThan(createTime, toExclusive);
            }
            if (toExclusive == null) {
                return cb.greaterThanOrEqualTo(createTime, fromInclusive);
            }
            return cb.and(cb.greaterThanOrEqualTo(createTime, fromInclusive),
                    cb.lessThan(createTime, toExclusive));
        };
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static <T extends Comparable<T>> Specification<Contact> between(String attribute, T from, T to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get(attribute), to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get(attribute), from);
            }
            return cb.between(root.get(attribute), from, to);
        };
    }
}
