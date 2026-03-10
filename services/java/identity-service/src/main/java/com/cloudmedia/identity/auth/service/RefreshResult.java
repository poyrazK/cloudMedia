package com.cloudmedia.identity.auth.service;

public record RefreshResult(String accessToken, String refreshToken, String sessionId) {
}
