package com.example.orm.jpa.dto;

import org.springframework.data.domain.Page;

import java.util.List;

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
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
