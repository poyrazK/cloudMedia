package com.cloudmedia.discovery.error;

import com.cloudmedia.discovery.api.response.ApiMeta;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	private final ApiMeta meta;

	public ApiException(HttpStatus status, String code, String message, ApiMeta meta) {
		super(message);
		this.status = status;
		this.code = code;
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
