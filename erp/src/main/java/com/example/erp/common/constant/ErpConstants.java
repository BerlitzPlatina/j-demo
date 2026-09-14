package com.example.erp.common.constant;

import java.util.Set;

/**
 * Values more than one feature depends on: paging defaults and the property
 * names inherited
 * from the audited base entity.
 * <p>
 * They are constants rather than literals because a page that silently changed
 * size, or a sort
 * allowlist that spelled an audit column differently from the entity, would
 * only surface as a
 * wrong answer at runtime.
 */
public final class ErpConstants {

    /** Page size applied when the request carries none. */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Paging needs a total order; the id is the one column guaranteed to provide
     * it.
     */
    public static final String ID = "id";

    /**
     * JPA property names on {@code AbstractAuditModel}, not the underscore column
     * names.
     */
    public static final String CREATE_TIME = "createdAt";
    public static final String LAST_UPDATE_TIME = "updatedAt";

    /**
     * Sort properties every audited entity accepts, to be unioned with its own
     * columns.
     */
    public static final Set<String> AUDIT_SORT_FIELDS = Set.of(ID, CREATE_TIME, LAST_UPDATE_TIME);

    private ErpConstants() {
    }
}
