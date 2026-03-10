package com.cloudmedia.identity.api;

import com.cloudmedia.identity.auth.config.AuthProperties;
import com.cloudmedia.identity.auth.service.AuthRefreshUseCase;
import com.cloudmedia.identity.api.dto.AuthTokensResponse;
import com.cloudmedia.identity.api.dto.LoginRequest;
import com.cloudmedia.identity.api.dto.LogoutRequest;
import com.cloudmedia.identity.api.dto.RefreshRequest;
import com.cloudmedia.identity.api.dto.SocialLoginRequest;
import com.cloudmedia.identity.api.response.ApiMeta;
import com.cloudmedia.identity.api.response.ApiSuccessResponse;
import com.cloudmedia.identity.error.ApiException;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

	private final AuthRefreshUseCase authRefreshService;
	private final AuthProperties authProperties;

	public AuthController(AuthRefreshUseCase authRefreshService, AuthProperties authProperties) {
		this.authRefreshService = authRefreshService;
		this.authProperties = authProperties;
	}

	@PostMapping("/login")
	public ResponseEntity<ApiSuccessResponse<AuthTokensResponse>> login(@Valid @RequestBody LoginRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "AUTH_NOT_IMPLEMENTED", "Login flow is not implemented yet",
				meta(requestId));
	}

	@PostMapping("/social-login")
	public ResponseEntity<ApiSuccessResponse<AuthTokensResponse>> socialLogin(
			@Valid @RequestBody SocialLoginRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "AUTH_NOT_IMPLEMENTED",
				"Social login flow is not implemented yet", meta(requestId));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiSuccessResponse<AuthTokensResponse>> refresh(@Valid @RequestBody RefreshRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		var result = authRefreshService.rotateRefreshToken(request.refreshToken());
		AuthTokensResponse response = new AuthTokensResponse(result.accessToken(), result.refreshToken(),
				result.sessionId(), authProperties.getAccessTokenTtl().toSeconds(),
				authProperties.getRefreshTokenTtl().toSeconds());
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiSuccessResponse<Void>> logout(@Valid @RequestBody LogoutRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "AUTH_NOT_IMPLEMENTED", "Logout flow is not implemented yet",
				meta(requestId));
	}

	private ApiMeta meta(String requestIdHeader) {
		String requestId = requestIdHeader != null && !requestIdHeader.isBlank()
				? requestIdHeader
				: "req_" + UUID.randomUUID();
		return new ApiMeta(requestId, Instant.now());
	}
}
