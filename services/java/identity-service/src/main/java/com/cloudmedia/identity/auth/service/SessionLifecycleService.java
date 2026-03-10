package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.persistence.entity.RefreshTokenEntity;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.repository.RefreshTokenRepository;
import com.cloudmedia.identity.persistence.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionLifecycleService {

	private final SessionRepository sessionRepository;
	private final RefreshTokenRepository refreshTokenRepository;

	public SessionLifecycleService(SessionRepository sessionRepository, RefreshTokenRepository refreshTokenRepository) {
		this.sessionRepository = sessionRepository;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	@Transactional
	public void enforceSessionCap(String userId, int maxActiveSessions, LocalDateTime now) {
		List<SessionEntity> activeSessions = sessionRepository.findByUser_IdAndRevokedAtIsNullOrderByCreatedAtAsc(userId);
		if (activeSessions.size() < maxActiveSessions) {
			return;
		}

		SessionEntity oldestSession = activeSessions.get(0);
		revokeSessionAndActiveTokens(oldestSession, now);
	}

	@Transactional
	public void revokeSessionAndActiveTokens(SessionEntity session, LocalDateTime now) {
		session.setRevokedAt(now);
		sessionRepository.save(session);

		List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(session.getId());
		for (RefreshTokenEntity token : activeTokens) {
			token.setRevokedAt(now);
		}
		refreshTokenRepository.saveAll(activeTokens);
	}
}
