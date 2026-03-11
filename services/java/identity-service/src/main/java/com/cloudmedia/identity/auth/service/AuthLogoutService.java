package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.error.ApiException;
import com.cloudmedia.identity.metrics.AuthMetrics;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.repository.SessionRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLogoutService implements AuthLogoutUseCase {

	private final SessionRepository sessionRepository;
	private final SessionLifecycleService sessionLifecycleService;
	private final AuthMetrics authMetrics;

	public AuthLogoutService(SessionRepository sessionRepository, SessionLifecycleService sessionLifecycleService,
			AuthMetrics authMetrics) {
		this.sessionRepository = sessionRepository;
		this.sessionLifecycleService = sessionLifecycleService;
		this.authMetrics = authMetrics;
	}

	@Override
	@Transactional
	public void logout(String sessionId, boolean allSessions) {
		if (sessionId == null || sessionId.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "LOGOUT_SESSION_REQUIRED", "Session id is required", null);
		}

		SessionEntity currentSession = sessionRepository.findById(sessionId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found", null));

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		if (allSessions) {
			List<SessionEntity> activeSessions = sessionRepository
					.findByUser_IdAndRevokedAtIsNull(currentSession.getUser().getId());
			for (SessionEntity session : activeSessions) {
				sessionLifecycleService.revokeSessionAndActiveTokens(session, now);
			}
			authMetrics.onLogoutSuccess();
			return;
		}

		sessionLifecycleService.revokeSessionAndActiveTokens(currentSession, now);
		authMetrics.onLogoutSuccess();
	}
}
