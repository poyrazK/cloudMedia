package com.cloudmedia.identity.persistence.repository;

import com.cloudmedia.identity.persistence.entity.OAuthAccountEntity;
import com.cloudmedia.identity.persistence.entity.OAuthProvider;
import com.cloudmedia.identity.persistence.entity.RefreshTokenEntity;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.entity.UserEntity;
import com.cloudmedia.identity.persistence.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class IdentityRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OAuthAccountRepository oAuthAccountRepository;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Test
	void usersEmailMustBeUnique() {
		UserEntity first = user("user@example.com");
		UserEntity second = user("user@example.com");

		userRepository.saveAndFlush(first);
		assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(second));
	}

	@Test
	void oauthProviderSubjectMustBeUnique() {
		UserEntity firstUser = userRepository.saveAndFlush(user("one@example.com"));
		UserEntity secondUser = userRepository.saveAndFlush(user("two@example.com"));

		oAuthAccountRepository.saveAndFlush(oauth(firstUser, OAuthProvider.GOOGLE, "sub-123"));

		assertThrows(DataIntegrityViolationException.class,
				() -> oAuthAccountRepository.saveAndFlush(oauth(secondUser, OAuthProvider.GOOGLE, "sub-123")));
	}

	@Test
	void findsOnlyActiveSessionsOrderedByCreatedAt() {
		UserEntity user = userRepository.saveAndFlush(user("session@example.com"));

		SessionEntity oldest = session(user, LocalDateTime.now().minusHours(3), null);
		SessionEntity revoked = session(user, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
		SessionEntity newest = session(user, LocalDateTime.now().minusHours(1), null);

		sessionRepository.saveAndFlush(oldest);
		sessionRepository.saveAndFlush(revoked);
		sessionRepository.saveAndFlush(newest);

		var active = sessionRepository.findByUser_IdAndRevokedAtIsNullOrderByCreatedAtAsc(user.getId());

		assertEquals(2, active.size());
		assertEquals(oldest.getId(), active.get(0).getId());
		assertEquals(newest.getId(), active.get(1).getId());
	}

	@Test
	void findsRefreshTokenByHash() {
		UserEntity user = userRepository.saveAndFlush(user("refresh@example.com"));
		SessionEntity session = sessionRepository.saveAndFlush(session(user, LocalDateTime.now(), null));
		RefreshTokenEntity token = refreshToken(session, "hash_123", null);

		refreshTokenRepository.saveAndFlush(token);

		var found = refreshTokenRepository.findByTokenHash("hash_123");
		assertTrue(found.isPresent());
		assertNotNull(found.get().getSession());
		assertEquals(session.getId(), found.get().getSession().getId());
	}

	@Test
	void findsOnlyUnrevokedRefreshTokensForSession() {
		UserEntity user = userRepository.saveAndFlush(user("refresh-list@example.com"));
		SessionEntity session = sessionRepository.saveAndFlush(session(user, LocalDateTime.now(), null));

		RefreshTokenEntity active = refreshToken(session, "hash_active", null);
		RefreshTokenEntity revoked = refreshToken(session, "hash_revoked", LocalDateTime.now());

		refreshTokenRepository.saveAndFlush(active);
		refreshTokenRepository.saveAndFlush(revoked);

		var tokens = refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(session.getId());
		assertEquals(1, tokens.size());
		assertEquals("hash_active", tokens.get(0).getTokenHash());
		assertFalse(tokens.get(0).getTokenHash().equals("hash_revoked"));
	}

	private UserEntity user(String email) {
		UserEntity entity = new UserEntity();
		entity.setId(uuid());
		entity.setEmail(email);
		entity.setStatus(UserStatus.ACTIVE);
		entity.setCreatedAt(LocalDateTime.now());
		entity.setUpdatedAt(LocalDateTime.now());
		return entity;
	}

	private OAuthAccountEntity oauth(UserEntity user, OAuthProvider provider, String subject) {
		OAuthAccountEntity entity = new OAuthAccountEntity();
		entity.setId(uuid());
		entity.setUser(user);
		entity.setProvider(provider);
		entity.setProviderSubject(subject);
		entity.setLinkedAt(LocalDateTime.now());
		return entity;
	}

	private SessionEntity session(UserEntity user, LocalDateTime createdAt, LocalDateTime revokedAt) {
		SessionEntity entity = new SessionEntity();
		entity.setId(uuid());
		entity.setUser(user);
		entity.setDeviceId("device-1");
		entity.setUserAgent("test-agent");
		entity.setIpAddress("127.0.0.1");
		entity.setCreatedAt(createdAt);
		entity.setExpiresAt(createdAt.plusDays(30));
		entity.setRevokedAt(revokedAt);
		return entity;
	}

	private RefreshTokenEntity refreshToken(SessionEntity session, String tokenHash, LocalDateTime revokedAt) {
		RefreshTokenEntity entity = new RefreshTokenEntity();
		entity.setId(uuid());
		entity.setSession(session);
		entity.setTokenHash(tokenHash);
		entity.setIssuedAt(LocalDateTime.now());
		entity.setExpiresAt(LocalDateTime.now().plusDays(30));
		entity.setRevokedAt(revokedAt);
		return entity;
	}

	private String uuid() {
		return UUID.randomUUID().toString();
	}
}
