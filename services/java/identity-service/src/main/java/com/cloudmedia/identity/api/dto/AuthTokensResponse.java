package com.cloudmedia.identity.api.dto;

public record AuthTokensResponse(String accessToken, String refreshToken, String sessionId,
		long accessTokenExpiresInSeconds, long refreshTokenExpiresInSeconds) {
}
