package com.closiq.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    boolean success;
    T data;
    ResponseMeta meta;

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(ResponseMeta.builder()
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String requestId, ResponseMeta.PaginationMeta pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(ResponseMeta.builder()
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .pagination(pagination)
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> okWithSearch(
            T data,
            String requestId,
            ResponseMeta.PaginationMeta pagination,
            ResponseMeta.SearchMeta searchMeta) {

        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(ResponseMeta.builder()
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .pagination(pagination)
                        .searchMeta(searchMeta)
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> okWithNotifications(
            T data,
            String requestId,
            ResponseMeta.PaginationMeta pagination,
            long unreadCount) {

        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(ResponseMeta.builder()
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .pagination(pagination)
                        .unreadCount(unreadCount)
                        .build())
                .build();
    }
}
