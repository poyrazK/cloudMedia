package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.api.dto.DeviceInfo;
import com.cloudmedia.identity.auth.config.AuthProperties;
import com.cloudmedia.identity.auth.password.PasswordHashService;
import com.cloudmedia.identity.error.ApiException;
import com.cloudmedia.identity.metrics.AuthMetrics;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.entity.UserCredentialEntity;
import com.cloudmedia.identity.persistence.entity.UserEntity;
import com.cloudmedia.identity.persistence.repository.SessionRepository;
import com.cloudmedia.identity.persistence.repository.UserCredentialRepository;
import com.cloudmedia.identity.persistence.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLoginService implements AuthLoginUseCase {

	private final AuthProperties authProperties;
	private final UserRepository userRepository;
	private final UserCredentialRepository userCredentialRepository;
	private final SessionRepository sessionRepository;
	private final SessionLifecycleService sessionLifecycleService;
	private final AuthTokenIssueService authTokenIssueService;
	private final PasswordHashService passwordHashService;
	private final AuthMetrics authMetrics;

	public AuthLoginService(AuthProperties authProperties, UserRepository userRepository,
			UserCredentialRepository userCredentialRepository, SessionRepository sessionRepository,
			SessionLifecycleService sessionLifecycleService, AuthTokenIssueService authTokenIssueService,
			PasswordHashService passwordHashService, AuthMetrics authMetrics) {
		this.authProperties = authProperties;
		this.userRepository = userRepository;
		this.userCredentialRepository = userCredentialRepository;
		this.sessionRepository = sessionRepository;
		this.sessionLifecycleService = sessionLifecycleService;
		this.authTokenIssueService = authTokenIssueService;
		this.passwordHashService = passwordHashService;
		this.authMetrics = authMetrics;
	}

	@Override
	@Transactional
	public RefreshResult login(String email, String password, DeviceInfo deviceInfo) {
		try {
			UserEntity user = userRepository.findByEmail(email)
					.orElseThrow(() -> unauthorized("AUTH_INVALID_CREDENTIALS", "Invalid email or password"));

			UserCredentialEntity credential = userCredentialRepository.findByUser_Id(user.getId())
					.orElseThrow(() -> unauthorized("AUTH_INVALID_CREDENTIALS", "Invalid email or password"));

			if (!passwordHashService.matches(password, credential.getPasswordHash())) {
				throw unauthorized("AUTH_INVALID_CREDENTIALS", "Invalid email or password");
			}

			LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
			sessionLifecycleService.enforceSessionCap(user.getId(), authProperties.getMaxActiveSessions(), now);

			SessionEntity session = new SessionEntity();
			session.setId(UUID.randomUUID().toString());
			session.setUser(user);
			session.setDeviceId(deviceInfo != null ? deviceInfo.deviceId() : null);
			session.setUserAgent(deviceInfo != null ? deviceInfo.userAgent() : null);
			session.setIpAddress(deviceInfo != null ? deviceInfo.ipAddress() : null);
			session.setCreatedAt(now);
			session.setExpiresAt(now.plus(authProperties.getRefreshTokenTtl()));

			SessionEntity savedSession = sessionRepository.save(session);
			RefreshResult result = authTokenIssueService.issueForSession(savedSession);
			authMetrics.onLoginSuccess();
			return result;
		} catch (ApiException exception) {
			authMetrics.onLoginFailure();
			throw exception;
		}
	}

	private ApiException unauthorized(String code, String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code, message, null);
	}
}
