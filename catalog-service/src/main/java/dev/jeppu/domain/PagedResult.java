package dev.jeppu.domain;

import java.util.List;

public record PagedResult<T>(
        long totalElements,
        int totalPages,
        int pageNumber,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious,
        List<T> data) {}
