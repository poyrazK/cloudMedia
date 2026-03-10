package com.cloudmedia.identity.auth.service;

public interface AuthRefreshUseCase {
	RefreshResult rotateRefreshToken(String rawRefreshToken);
}
