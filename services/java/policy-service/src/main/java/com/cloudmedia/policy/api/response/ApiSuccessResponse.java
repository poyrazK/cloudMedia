package com.cloudmedia.policy.api.response;

public record ApiSuccessResponse<T>(T data, ApiMeta meta) {
}
