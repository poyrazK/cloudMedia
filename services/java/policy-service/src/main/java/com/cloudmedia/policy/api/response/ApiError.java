package com.cloudmedia.policy.api.response;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> details) {

	public ApiError {
		details = details == null ? Map.of() : Map.copyOf(details);
	}
}
