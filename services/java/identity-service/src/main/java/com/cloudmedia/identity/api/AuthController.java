package com.cloudmedia.identity.api;

import com.cloudmedia.identity.auth.config.AuthProperties;
import com.cloudmedia.identity.auth.service.AuthLoginUseCase;
import com.cloudmedia.identity.auth.service.AuthLogoutUseCase;
import com.cloudmedia.identity.auth.service.AuthRefreshUseCase;
import com.cloudmedia.identity.auth.service.AuthSocialLoginUseCase;
import com.cloudmedia.identity.api.dto.AuthTokensResponse;
import com.cloudmedia.identity.api.dto.LoginRequest;
import com.cloudmedia.identity.api.dto.LogoutRequest;
import com.cloudmedia.identity.api.dto.RefreshRequest;
import com.cloudmedia.identity.api.dto.SocialLoginRequest;
import com.cloudmedia.identity.api.response.ApiMeta;
import com.cloudmedia.identity.api.response.ApiSuccessResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

	private final AuthLoginUseCase authLoginService;
	private final AuthSocialLoginUseCase authSocialLoginService;
	private final AuthRefreshUseCase authRefreshService;
	private final AuthLogoutUseCase authLogoutService;
	private final AuthProperties authProperties;

	public AuthController(AuthLoginUseCase authLoginService, AuthSocialLoginUseCase authSocialLoginService,
			AuthRefreshUseCase authRefreshService, AuthLogoutUseCase authLogoutService, AuthProperties authProperties) {
		this.authLoginService = authLoginService;
		this.authSocialLoginService = authSocialLoginService;
		this.authRefreshService = authRefreshService;
		this.authLogoutService = authLogoutService;
		this.authProperties = authProperties;
	}

	@PostMapping("/login")
	public ResponseEntity<ApiSuccessResponse<AuthTokensResponse>> login(@Valid @RequestBody LoginRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		var result = authLoginService.login(request.email(), request.password(), request.deviceInfo());
		AuthTokensResponse response = toAuthTokensResponse(result);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@PostMapping("/social-login")
	public ResponseEntity<ApiSuccessResponse<AuthTokensResponse>> socialLogin(
			@Valid @RequestBody SocialLoginRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		var result = authSocialLoginService.socialLogin(request.provider(), request.providerToken(),
				request.deviceInfo());
		AuthTokensResponse response = toAuthTokensResponse(result);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
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
		authLogoutService.logout(request.sessionId(), request.allSessions());
		return ResponseEntity.ok(new ApiSuccessResponse<>(null, meta(requestId)));
	}

	private AuthTokensResponse toAuthTokensResponse(com.cloudmedia.identity.auth.service.RefreshResult result) {
		return new AuthTokensResponse(result.accessToken(), result.refreshToken(), result.sessionId(),
				authProperties.getAccessTokenTtl().toSeconds(), authProperties.getRefreshTokenTtl().toSeconds());
	}

	private ApiMeta meta(String requestIdHeader) {
		String requestId = requestIdHeader != null && !requestIdHeader.isBlank()
				? requestIdHeader
				: "req_" + UUID.randomUUID();
		return new ApiMeta(requestId, Instant.now());
	}
}
