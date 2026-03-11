package com.cloudmedia.content.api.response;

public record ApiSuccessResponse<T>(T data, ApiMeta meta) {
}
