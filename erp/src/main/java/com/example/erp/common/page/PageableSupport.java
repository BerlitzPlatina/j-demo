package com.example.erp.common.page;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.erp.common.constant.ErpConstants;

/**
 * Turns the {@code sort} a client sent into one the repository may run.
 * <p>
 * Spring Data translates a sort property straight into a path on the entity, so an unchecked
 * value either reaches the SQL or blows up as a 500 deep inside the query. Checking it against
 * an allowlist first turns that into a 400 naming the property.
 */
public final class PageableSupport {

    /** Newest first, and - since the id is unique - a total order, so pages cannot overlap. */
    public static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, ErpConstants.ID);

    private PageableSupport() {
    }

    /**
     * @param sortableFields JPA property names the entity accepts, usually its own columns
     *                       unioned with {@link ErpConstants#AUDIT_SORT_FIELDS}
     * @return the same paging, sorted by {@link #DEFAULT_SORT} when the request carried no sort
     * @throws IllegalArgumentException if a sort property is not in the allowlist
     */
    public static Pageable sanitize(Pageable pageable, Set<String> sortableFields) {
        return sanitize(pageable, sortableFields, DEFAULT_SORT);
    }

    /** As {@link #sanitize(Pageable, Set)}, for an entity that needs its own fallback order. */
    public static Pageable sanitize(Pageable pageable, Set<String> sortableFields, Sort defaultSort) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }
        sort.forEach(order -> {
            if (!sortableFields.contains(order.getProperty())) {
                throw new IllegalArgumentException("sort: unsupported property '" + order.getProperty()
                        + "', allowed: " + sortableFields);
            }
        });
        return pageable;
    }

    /** The allowlist of an audited entity: its own properties plus id and the two timestamps. */
    public static Set<String> sortableFields(String... ownProperties) {
        return Stream.concat(ErpConstants.AUDIT_SORT_FIELDS.stream(), Arrays.stream(ownProperties))
                .collect(Collectors.toUnmodifiableSet());
    }
}
