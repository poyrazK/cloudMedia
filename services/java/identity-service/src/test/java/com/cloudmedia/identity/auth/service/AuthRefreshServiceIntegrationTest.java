package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.auth.token.RefreshTokenHasher;
import com.cloudmedia.identity.error.ApiException;
import com.cloudmedia.identity.persistence.entity.RefreshTokenEntity;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.entity.UserEntity;
import com.cloudmedia.identity.persistence.entity.UserStatus;
import com.cloudmedia.identity.persistence.repository.RefreshTokenRepository;
import com.cloudmedia.identity.persistence.repository.SessionRepository;
import com.cloudmedia.identity.persistence.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuthRefreshServiceIntegrationTest {

	@Autowired
	private AuthRefreshService authRefreshService;

	@Autowired
	private SessionLifecycleService sessionLifecycleService;

	@Autowired
	private AuthTokenIssueService authTokenIssueService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private RefreshTokenHasher refreshTokenHasher;

	@Test
	void rotatesRefreshTokenAndRevokesPreviousToken() {
		UserEntity user = saveUser("rotate@example.com");
		SessionEntity session = saveSession(user, LocalDateTime.now().minusMinutes(5));

		RefreshResult issued = authTokenIssueService.issueForSession(session);
		RefreshResult rotated = authRefreshService.rotateRefreshToken(issued.refreshToken());

		assertNotEquals(issued.refreshToken(), rotated.refreshToken());
		assertNotNull(rotated.accessToken());

		String oldTokenHash = refreshTokenHasher.hash(issued.refreshToken());
		RefreshTokenEntity reloadedOldToken = refreshTokenRepository.findByTokenHash(oldTokenHash).orElseThrow();
		assertNotNull(reloadedOldToken.getRevokedAt());

		var activeTokens = refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(session.getId());
		assertEquals(1, activeTokens.size());
		assertEquals(refreshTokenHasher.hash(rotated.refreshToken()), activeTokens.get(0).getTokenHash());
	}

	@Test
	void detectsRefreshTokenReuseAndRevokesSession() {
		UserEntity user = saveUser("reuse@example.com");
		SessionEntity session = saveSession(user, LocalDateTime.now().minusMinutes(5));

		RefreshResult issued = authTokenIssueService.issueForSession(session);
		authRefreshService.rotateRefreshToken(issued.refreshToken());

		ApiException exception = assertThrows(ApiException.class,
				() -> authRefreshService.rotateRefreshToken(issued.refreshToken()));

		assertEquals("REFRESH_TOKEN_REUSED", exception.getCode());
		SessionEntity reloaded = sessionRepository.findById(session.getId()).orElseThrow();
		assertNotNull(reloaded.getRevokedAt());
	}

	@Test
	void enforcesSessionCapByRevokingOldestSession() {
		UserEntity user = saveUser("cap@example.com");

		for (int i = 0; i < 5; i++) {
			saveSession(user, LocalDateTime.now().minusMinutes(10 - i));
		}

		sessionLifecycleService.enforceSessionCap(user.getId(), 5, LocalDateTime.now());

		var activeSessions = sessionRepository.findByUser_IdAndRevokedAtIsNullOrderByCreatedAtAsc(user.getId());
		assertEquals(4, activeSessions.size());

		long revokedCount = sessionRepository.findAll().stream().filter(session -> session.getRevokedAt() != null).count();
		assertTrue(revokedCount >= 1);
	}

	private UserEntity saveUser(String email) {
		UserEntity user = new UserEntity();
		user.setId(uuid());
		user.setEmail(email);
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		return userRepository.saveAndFlush(user);
	}

	private SessionEntity saveSession(UserEntity user, LocalDateTime createdAt) {
		SessionEntity session = new SessionEntity();
		session.setId(uuid());
		session.setUser(user);
		session.setDeviceId("device-1");
		session.setUserAgent("test-agent");
		session.setIpAddress("127.0.0.1");
		session.setCreatedAt(createdAt);
		session.setExpiresAt(createdAt.plusDays(30));
		return sessionRepository.saveAndFlush(session);
	}

	private String uuid() {
		return UUID.randomUUID().toString();
	}
}
