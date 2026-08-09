package com.closiq.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMeta {

    String requestId;
    Instant timestamp;
    PaginationMeta pagination;
    SearchMeta searchMeta;
    Long unreadCount;

    @Value
    @Builder
    public static class SearchMeta {
        String query;
        Long totalCount;
        Long tookMs;
    }

    @Value
    @Builder
    public static class PaginationMeta {
        String type;
        Integer limit;
        String nextPageToken;
        String prevPageToken;
        Boolean hasMore;
        Long totalCount;
    }
}
