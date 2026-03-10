package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.api.dto.DeviceInfo;
import com.cloudmedia.identity.api.dto.SocialProvider;
import com.cloudmedia.identity.auth.password.PasswordHashService;
import com.cloudmedia.identity.error.ApiException;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.entity.UserCredentialEntity;
import com.cloudmedia.identity.persistence.entity.UserEntity;
import com.cloudmedia.identity.persistence.entity.UserStatus;
import com.cloudmedia.identity.persistence.repository.OAuthAccountRepository;
import com.cloudmedia.identity.persistence.repository.RefreshTokenRepository;
import com.cloudmedia.identity.persistence.repository.SessionRepository;
import com.cloudmedia.identity.persistence.repository.UserCredentialRepository;
import com.cloudmedia.identity.persistence.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuthLoginSocialLogoutIntegrationTest {

	@Autowired
	private AuthLoginUseCase authLoginUseCase;

	@Autowired
	private AuthSocialLoginUseCase authSocialLoginUseCase;

	@Autowired
	private AuthLogoutUseCase authLogoutUseCase;

	@Autowired
	private PasswordHashService passwordHashService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCredentialRepository userCredentialRepository;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private OAuthAccountRepository oAuthAccountRepository;

	@Test
	void loginCreatesSessionAndReturnsTokens() {
		seedCredentialUser("login@example.com", "password123");

		RefreshResult result = authLoginUseCase.login("login@example.com", "password123",
				new DeviceInfo("d1", "ios", "1.0.0", "ua", "127.0.0.1"));

		assertNotNull(result.accessToken());
		assertNotNull(result.refreshToken());
		assertNotNull(result.sessionId());
		assertEquals(1, sessionRepository.findAll().size());
		assertEquals(1, refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(result.sessionId()).size());
	}

	@Test
	void loginWithInvalidPasswordFails() {
		seedCredentialUser("login-fail@example.com", "password123");

		ApiException exception = assertThrows(ApiException.class,
				() -> authLoginUseCase.login("login-fail@example.com", "wrong-password", null));
		assertEquals("AUTH_INVALID_CREDENTIALS", exception.getCode());
	}

	@Test
	void socialLoginCreatesUserAndOauthLinkWithFakeGoogleToken() {
		RefreshResult result = authSocialLoginUseCase.socialLogin(SocialProvider.GOOGLE,
				"fake-google:sub-123:social@example.com", null);

		assertNotNull(result.accessToken());
		assertEquals(1, userRepository.findAll().size());
		assertEquals(1, oAuthAccountRepository.findAll().size());
	}

	@Test
	void logoutAllSessionsRevokesAllActiveSessionsAndTokens() {
		UserEntity user = seedCredentialUser("logout@example.com", "password123");

		RefreshResult first = authLoginUseCase.login("logout@example.com", "password123", null);
		RefreshResult second = authLoginUseCase.login("logout@example.com", "password123", null);

		authLogoutUseCase.logout(first.sessionId(), true);

		assertEquals(0, sessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).size());
		assertTrue(refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(first.sessionId()).isEmpty());
		assertTrue(refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(second.sessionId()).isEmpty());

		SessionEntity reloadedFirst = sessionRepository.findById(first.sessionId()).orElseThrow();
		SessionEntity reloadedSecond = sessionRepository.findById(second.sessionId()).orElseThrow();
		assertNotNull(reloadedFirst.getRevokedAt());
		assertNotNull(reloadedSecond.getRevokedAt());
	}

	@Test
	void logoutCurrentSessionRevokesOnlyCurrentSession() {
		UserEntity user = seedCredentialUser("logout-single@example.com", "password123");

		RefreshResult first = authLoginUseCase.login("logout-single@example.com", "password123", null);
		RefreshResult second = authLoginUseCase.login("logout-single@example.com", "password123", null);

		authLogoutUseCase.logout(first.sessionId(), false);

		assertEquals(1, sessionRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).size());
		assertTrue(refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(first.sessionId()).isEmpty());
		assertFalse(refreshTokenRepository.findBySession_IdAndRevokedAtIsNull(second.sessionId()).isEmpty());
	}

	private UserEntity seedCredentialUser(String email, String rawPassword) {
		UserEntity user = new UserEntity();
		user.setId(UUID.randomUUID().toString());
		user.setEmail(email);
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		user = userRepository.saveAndFlush(user);

		UserCredentialEntity credential = new UserCredentialEntity();
		credential.setUser(user);
		credential.setPasswordHash(passwordHashService.hash(rawPassword));
		credential.setUpdatedAt(LocalDateTime.now());
		userCredentialRepository.saveAndFlush(credential);

		return user;
	}
}
