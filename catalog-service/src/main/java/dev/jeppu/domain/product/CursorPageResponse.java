package dev.jeppu.domain.product;

import java.util.List;

/**
 * A cursor-based page of results.
 *
 * <p>{@code nextCursor} is an opaque token representing the position just after the last
 * returned item. Clients pass it back as the {@code cursor} request parameter to fetch the
 * next page; {@code null} means there is no more data.
 */
public record CursorPageResponse<T>(List<T> data, String nextCursor, int size) {}
