package com.example.orm.jpa.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Serialization-friendly view of a {@link Page}.
 * Spring's own Page implementation is not a stable JSON contract, so we map it explicitly.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return from(page, Function.identity());
    }

    /**
     * Wraps the page metadata while mapping the entity content to a response type,
     * so the entity itself never reaches the JSON layer.
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    /** Same metadata as {@code page}, but with content supplied separately (already mapped and ordered). */
    public static <E, T> PageResponse<T> of(Page<E> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
