package com.cloudmedia.identity.error;

import com.cloudmedia.identity.api.response.ApiErrorResponse;
import com.cloudmedia.identity.api.response.ApiMeta;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void returnsApiExceptionEnvelopeWithProvidedStatusAndCode() {
		ApiMeta meta = new ApiMeta("req_fixed_1", Instant.parse("2026-03-09T12:00:00Z"));
		ApiException ex = new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "Access token expired", meta);

		var response = handler.handleApiException(ex);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		ApiErrorResponse body = response.getBody();
		assertNotNull(body);
		assertEquals("AUTH_TOKEN_EXPIRED", body.error().code());
		assertEquals("Access token expired", body.error().message());
		assertEquals("req_fixed_1", body.meta().requestId());
	}

	@Test
	void returnsInternalErrorEnvelopeForUnhandledException() {
		var response = handler.handleUnhandled(new IllegalStateException("boom"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		ApiErrorResponse body = response.getBody();
		assertNotNull(body);
		assertEquals("INTERNAL_ERROR", body.error().code());
		assertFalse(body.meta().requestId().isBlank());
	}
}
