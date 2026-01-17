package dev.jeppu.bookstore.domain;

import java.util.List;

public record PageResponse<T>(
        long totalElements,
        int totalPages,
        int pageNumber,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious,
        List<T> data) {}
