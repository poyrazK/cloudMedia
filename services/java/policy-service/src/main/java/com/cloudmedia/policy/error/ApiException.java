package com.cloudmedia.policy.error;

import com.cloudmedia.policy.api.response.ApiMeta;
import java.util.Objects;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final ApiMeta meta;

	public ApiException(HttpStatus status, String code, String message, ApiMeta meta) {
		super(message);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.code = Objects.requireNonNull(code, "code must not be null");
		this.meta = meta;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public ApiMeta getMeta() {
		return meta;
	}
}
