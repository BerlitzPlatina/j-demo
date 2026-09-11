package com.example.erp.common.specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Builds the {@code where} of a search out of optional filters.
 * <p>
 * The point is what it does with an absent value: a null - or a blank string, or an empty
 * collection - adds nothing at all, so the filter never reaches the SQL. Collecting the
 * conditions first also keeps nulls away from {@link Specification#allOf}, which answers
 * "Other specification must not be null" rather than skipping them, so a request that leaves
 * any filter out would otherwise fail with a 400.
 *
 * <pre>
 * return SpecBuilder.&lt;Tax&gt;of()
 *         .like("name", request.name())
 *         .eq("type", request.type())
 *         .between("startDate", request.startFrom(), request.startTo())
 *         .build();
 * </pre>
 *
 * @param <T> the entity being searched
 */
public final class SpecBuilder<T> {

    private final List<Specification<T>> conditions = new ArrayList<>();

    private SpecBuilder() {
    }

    public static <T> SpecBuilder<T> of() {
        return new SpecBuilder<>();
    }

    /**
     * Adds a condition the builder does not model - a subquery, an or-group - written by hand.
     * A null is ignored, so a feature helper can return null to mean "filter not requested"
     * just like the built-in ones.
     */
    public SpecBuilder<T> and(Specification<T> specification) {
        if (specification != null) {
            conditions.add(specification);
        }
        return this;
    }

    /** {@code attribute = value}. */
    public SpecBuilder<T> eq(String attribute, Object value) {
        if (value == null) {
            return this;
        }
        return and((root, query, cb) -> cb.equal(root.get(attribute), value));
    }

    /** {@code lower(attribute) like %keyword%}. */
    public SpecBuilder<T> like(String attribute, String keyword) {
        String pattern = Specs.pattern(keyword);
        if (pattern == null) {
            return this;
        }
        return and((root, query, cb) -> Specs.likeLower(cb, root.get(attribute), pattern));
    }

    /**
     * One search box spread over several columns: the row matches when any of them does.
     * <p>
     * For a keyword that also has to reach a related table, add the subquery with
     * {@link #and(Specification)} instead - see {@code ContactSpecifications}.
     */
    public SpecBuilder<T> likeAny(String keyword, String... attributes) {
        String pattern = Specs.pattern(keyword);
        if (pattern == null || attributes.length == 0) {
            return this;
        }
        return and((root, query, cb) -> {
            List<Predicate> matches = new ArrayList<>(attributes.length);
            for (String attribute : attributes) {
                matches.add(Specs.likeLower(cb, root.get(attribute), pattern));
            }
            return cb.or(matches.toArray(Predicate[]::new));
        });
    }

    /** {@code attribute in (values)}. */
    public SpecBuilder<T> in(String attribute, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        return and((root, query, cb) -> root.get(attribute).in(values));
    }

    /**
     * A closed, half-open or open range, depending on which bounds the caller supplied; both
     * given bounds are inclusive.
     */
    public <V extends Comparable<? super V>> SpecBuilder<T> between(String attribute, V from, V to) {
        if (from == null && to == null) {
            return this;
        }
        return and((root, query, cb) -> {
            Path<V> path = root.get(attribute);
            if (from == null) {
                return cb.lessThanOrEqualTo(path, to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(path, from);
            }
            return cb.between(path, from, to);
        });
    }

    /**
     * A day range over a timestamp column - the audit columns, typically. The caller thinks in
     * whole days, so "to" is inclusive: it is compared as the start of the following day, which
     * is what keeps rows recorded during that day in the result.
     */
    public SpecBuilder<T> dayRange(String attribute, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return this;
        }
        Date fromInclusive = Specs.startOfDay(from);
        Date toExclusive = Specs.startOfNextDay(to);
        return and((root, query, cb) -> {
            Path<Date> path = root.get(attribute);
            if (fromInclusive == null) {
                return cb.lessThan(path, toExclusive);
            }
            if (toExclusive == null) {
                return cb.greaterThanOrEqualTo(path, fromInclusive);
            }
            return cb.and(cb.greaterThanOrEqualTo(path, fromInclusive), cb.lessThan(path, toExclusive));
        });
    }

    /** An empty request means "no filtering", not "no rows". */
    public Specification<T> build() {
        return conditions.isEmpty() ? Specification.unrestricted() : Specification.allOf(conditions);
    }
}
