package com.cloudmedia.discovery.api.response;

public record ApiSuccessResponse<T>(T data, ApiMeta meta) {
}
