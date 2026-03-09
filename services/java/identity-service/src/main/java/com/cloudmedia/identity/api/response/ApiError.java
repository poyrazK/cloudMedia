package com.cloudmedia.identity.api.response;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> details) {
}
