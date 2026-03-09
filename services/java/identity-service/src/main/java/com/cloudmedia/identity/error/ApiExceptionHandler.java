package com.cloudmedia.identity.error;

import com.cloudmedia.identity.api.response.ApiError;
import com.cloudmedia.identity.api.response.ApiErrorResponse;
import com.cloudmedia.identity.api.response.ApiMeta;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			details.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		return ResponseEntity.badRequest().body(
				new ApiErrorResponse(new ApiError("VALIDATION_ERROR", "Request validation failed", details), meta()));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getConstraintViolations()
				.forEach(violation -> details.put(violation.getPropertyPath().toString(), violation.getMessage()));

		return ResponseEntity.badRequest().body(
				new ApiErrorResponse(new ApiError("VALIDATION_ERROR", "Request validation failed", details), meta()));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
		ApiMeta responseMeta = exception.getMeta() != null ? exception.getMeta() : meta();
		ApiErrorResponse response = new ApiErrorResponse(
				new ApiError(exception.getCode(), exception.getMessage(), Map.of()), responseMeta);
		return ResponseEntity.status(exception.getStatus()).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnhandled(Exception exception) {
		ApiErrorResponse response = new ApiErrorResponse(
				new ApiError("INTERNAL_ERROR", "Unexpected server error", Map.of()), meta());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}

	private ApiMeta meta() {
		return new ApiMeta("req_" + UUID.randomUUID(), Instant.now());
	}
}
