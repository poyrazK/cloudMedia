package com.cloudmedia.identity.api.response;

public record ApiSuccessResponse<T>(T data, ApiMeta meta) {
}
