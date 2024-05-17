package dev.jeppu.bookstore.catalog.domain;

import java.util.List;

public record PagedResponse<T>(long totalElements,
                               int totalPages,
                               int pageNumber,
                               boolean isFirst,
                               boolean isLast,
                               boolean hasNext,
                               boolean hasPrevious,
                               List<T> data) {
}
