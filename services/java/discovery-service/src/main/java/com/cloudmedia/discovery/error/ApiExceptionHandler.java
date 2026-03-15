package com.cloudmedia.discovery.error;

import com.cloudmedia.discovery.api.response.ApiError;
import com.cloudmedia.discovery.api.response.ApiErrorResponse;
import com.cloudmedia.discovery.api.response.ApiMeta;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			mergeDetail(details, fieldError.getField(), fieldError.getDefaultMessage());
		}
		return ResponseEntity.badRequest().body(
				new ApiErrorResponse(new ApiError("VALIDATION_ERROR", "Request validation failed", details), meta()));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getConstraintViolations()
				.forEach(v -> mergeDetail(details, v.getPropertyPath().toString(), v.getMessage()));
		return ResponseEntity.badRequest().body(
				new ApiErrorResponse(new ApiError("VALIDATION_ERROR", "Request validation failed", details), meta()));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
		ApiMeta responseMeta = exception.getMeta() != null ? exception.getMeta() : meta();
		return ResponseEntity.status(exception.getStatus()).body(new ApiErrorResponse(
				new ApiError(exception.getCode(), exception.getMessage(), Map.of()), responseMeta));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnhandled(Exception exception) {
		LOGGER.error("Unhandled exception in ApiExceptionHandler", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				new ApiErrorResponse(new ApiError("INTERNAL_ERROR", "Unexpected server error", Map.of()), meta()));
	}

	private void mergeDetail(Map<String, String> details, String key, String message) {
		details.merge(key, message,
				(existing, incoming) -> existing.contains(incoming) ? existing : existing + "; " + incoming);
	}

	private ApiMeta meta() {
		return new ApiMeta("req_" + UUID.randomUUID(), Instant.now());
	}
}
