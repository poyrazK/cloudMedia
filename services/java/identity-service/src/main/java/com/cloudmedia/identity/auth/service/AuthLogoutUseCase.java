package com.cloudmedia.identity.auth.service;

public interface AuthLogoutUseCase {
	void logout(String sessionId, boolean allSessions);
}
