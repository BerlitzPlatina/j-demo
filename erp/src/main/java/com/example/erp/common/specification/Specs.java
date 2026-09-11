package com.example.erp.common.specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.BiFunction;

import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * The criteria-API plumbing the feature specifications share: pattern building, the
 * timestamp conversion and correlated {@code exists} subqueries.
 * <p>
 * {@link SpecBuilder} covers the ordinary filters; this class is what a feature reaches for
 * when it needs a predicate the builder does not model, so that even a hand-written condition
 * treats case and date boundaries the same way as the rest.
 */
public final class Specs {

    private Specs() {
    }

    /**
     * The {@code %keyword%} form used by every text filter: trimmed and lower-cased, so the
     * comparison stays case-insensitive whatever the column collation is.
     *
     * @return null when the keyword carries no text, which callers read as "no filter"
     */
    public static String pattern(String keyword) {
        return StringUtils.hasText(keyword) ? "%" + keyword.trim().toLowerCase() + "%" : null;
    }

    /** {@code lower(expression) like pattern}; the pattern must already come from {@link #pattern}. */
    public static Predicate likeLower(CriteriaBuilder cb, Expression<String> expression, String pattern) {
        return cb.like(cb.lower(expression), pattern);
    }

    /**
     * <pre>exists (select 1 from RELATED r where ...)</pre>
     * <p>
     * A subquery rather than a join on purpose: the outer query still selects one row per root
     * entity, so the condition can be composed with any other without multiplying or dropping
     * rows, and a null foreign key simply fails it instead of needing an outer join.
     *
     * @param where receives the subquery root and the builder, and returns the correlation plus
     *              whatever the caller filters on
     */
    public static <R> Predicate exists(CriteriaQuery<?> query, CriteriaBuilder cb, Class<R> relatedType,
            BiFunction<Root<R>, CriteriaBuilder, Predicate> where) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<R> related = subquery.from(relatedType);
        return cb.exists(subquery.select(cb.literal(1)).where(where.apply(related, cb)));
    }

    /**
     * Start of the given day, in the JVM zone, as the {@code java.util.Date} a
     * {@code DATETIME} column is mapped to.
     */
    public static Date startOfDay(LocalDate date) {
        return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Start of the day after the given one - what an inclusive "to" on a timestamp column has
     * to compare against, since 00:00 of the day itself would drop everything recorded during it.
     */
    public static Date startOfNextDay(LocalDate date) {
        return date == null ? null : startOfDay(date.plusDays(1));
    }

}
