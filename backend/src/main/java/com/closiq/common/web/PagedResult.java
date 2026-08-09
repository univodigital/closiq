package com.closiq.common.web;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PagedResult<T> {

    List<T> items;
    String nextPageToken;
    String prevPageToken;
    boolean hasMore;
    int limit;

    public static <T> PagedResult<T> of(List<T> items, int limit, boolean hasMore, String nextPageToken) {
        return PagedResult.<T>builder()
                .items(items)
                .limit(limit)
                .hasMore(hasMore)
                .nextPageToken(nextPageToken)
                .prevPageToken(null)
                .build();
    }
}
