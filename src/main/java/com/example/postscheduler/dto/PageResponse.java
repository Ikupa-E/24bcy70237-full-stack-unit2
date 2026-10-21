package com.example.postscheduler.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/** Generic wrapper that carries one page of data plus the paging metadata a client needs. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious,
        String sortBy,
        String direction
) {
    public static <T> PageResponse<T> of(Page<T> p, String sortBy, String direction) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(),
                p.getTotalPages(), p.isFirst(), p.isLast(), p.hasNext(), p.hasPrevious(),
                sortBy, direction);
    }
}
