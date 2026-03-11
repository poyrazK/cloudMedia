package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.api.dto.DeviceInfo;
import com.cloudmedia.identity.api.dto.SocialProvider;
import com.cloudmedia.identity.auth.config.AuthProperties;
import com.cloudmedia.identity.auth.social.GoogleIdentity;
import com.cloudmedia.identity.auth.social.GoogleTokenVerifier;
import com.cloudmedia.identity.error.ApiException;
import com.cloudmedia.identity.events.IdentityEventEnvelope;
import com.cloudmedia.identity.events.IdentityEventPublisher;
import com.cloudmedia.identity.events.UserCreatedPayload;
import com.cloudmedia.identity.events.UserUpdatedPayload;
import com.cloudmedia.identity.metrics.AuthMetrics;
import com.cloudmedia.identity.persistence.entity.OAuthAccountEntity;
import com.cloudmedia.identity.persistence.entity.OAuthProvider;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.entity.UserEntity;
import com.cloudmedia.identity.persistence.entity.UserStatus;
import com.cloudmedia.identity.persistence.repository.OAuthAccountRepository;
import com.cloudmedia.identity.persistence.repository.SessionRepository;
import com.cloudmedia.identity.persistence.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSocialLoginService implements AuthSocialLoginUseCase {

	private final AuthProperties authProperties;
	private final GoogleTokenVerifier googleTokenVerifier;
	private final UserRepository userRepository;
	private final OAuthAccountRepository oAuthAccountRepository;
	private final SessionRepository sessionRepository;
	private final SessionLifecycleService sessionLifecycleService;
	private final AuthTokenIssueService authTokenIssueService;
	private final IdentityEventPublisher identityEventPublisher;
	private final AuthMetrics authMetrics;

	public AuthSocialLoginService(AuthProperties authProperties, GoogleTokenVerifier googleTokenVerifier,
			UserRepository userRepository, OAuthAccountRepository oAuthAccountRepository,
			SessionRepository sessionRepository, SessionLifecycleService sessionLifecycleService,
			AuthTokenIssueService authTokenIssueService, IdentityEventPublisher identityEventPublisher,
			AuthMetrics authMetrics) {
		this.authProperties = authProperties;
		this.googleTokenVerifier = googleTokenVerifier;
		this.userRepository = userRepository;
		this.oAuthAccountRepository = oAuthAccountRepository;
		this.sessionRepository = sessionRepository;
		this.sessionLifecycleService = sessionLifecycleService;
		this.authTokenIssueService = authTokenIssueService;
		this.identityEventPublisher = identityEventPublisher;
		this.authMetrics = authMetrics;
	}

	@Override
	@Transactional
	public RefreshResult socialLogin(SocialProvider provider, String providerToken, DeviceInfo deviceInfo) {
		try {
			if (provider != SocialProvider.GOOGLE) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "SOCIAL_PROVIDER_UNSUPPORTED",
						"Only GOOGLE provider is supported", null);
			}

			GoogleIdentity googleIdentity = googleTokenVerifier.verify(providerToken);
			SocialResolution resolution = resolveOrCreateUser(googleIdentity);

			LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
			sessionLifecycleService.enforceSessionCap(resolution.user().getId(), authProperties.getMaxActiveSessions(),
					now);

			SessionEntity session = new SessionEntity();
			session.setId(UUID.randomUUID().toString());
			session.setUser(resolution.user());
			session.setDeviceId(deviceInfo != null ? deviceInfo.deviceId() : null);
			session.setUserAgent(deviceInfo != null ? deviceInfo.userAgent() : null);
			session.setIpAddress(deviceInfo != null ? deviceInfo.ipAddress() : null);
			session.setCreatedAt(now);
			session.setExpiresAt(now.plus(authProperties.getRefreshTokenTtl()));

			SessionEntity savedSession = sessionRepository.save(session);
			publishIdentityEvent(resolution);
			RefreshResult result = authTokenIssueService.issueForSession(savedSession);
			authMetrics.onSocialLoginSuccess();
			return result;
		} catch (ApiException exception) {
			authMetrics.onSocialLoginFailure();
			throw exception;
		}
	}

	private SocialResolution resolveOrCreateUser(GoogleIdentity identity) {
		return oAuthAccountRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, identity.subject())
				.map(account -> new SocialResolution(account.getUser(), false, false))
				.orElseGet(() -> createOrLinkUser(identity));
	}

	private SocialResolution createOrLinkUser(GoogleIdentity identity) {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		boolean[] createdNewUser = new boolean[]{false};

		UserEntity user = userRepository.findByEmail(identity.email()).orElseGet(() -> {
			createdNewUser[0] = true;
			UserEntity newUser = new UserEntity();
			newUser.setId(UUID.randomUUID().toString());
			newUser.setEmail(identity.email());
			newUser.setStatus(UserStatus.ACTIVE);
			newUser.setCreatedAt(now);
			newUser.setUpdatedAt(now);
			return userRepository.save(newUser);
		});

		OAuthAccountEntity account = new OAuthAccountEntity();
		account.setId(UUID.randomUUID().toString());
		account.setUser(user);
		account.setProvider(OAuthProvider.GOOGLE);
		account.setProviderSubject(identity.subject());
		account.setLinkedAt(now);
		oAuthAccountRepository.save(account);

		boolean linkedExistingUser = !createdNewUser[0];
		return new SocialResolution(user, createdNewUser[0], linkedExistingUser);
	}

	private void publishIdentityEvent(SocialResolution resolution) {
		if (resolution.createdUser()) {
			identityEventPublisher.publish(new IdentityEventEnvelope(UUID.randomUUID().toString(), "user.created", 1,
					Instant.now(), "identity-service", "user", resolution.user().getId(), null, new UserCreatedPayload(
							resolution.user().getId(), resolution.user().getEmail(), "google-social-login")));
			return;
		}

		if (resolution.linkedExistingUser()) {
			identityEventPublisher.publish(new IdentityEventEnvelope(UUID.randomUUID().toString(), "user.updated", 1,
					Instant.now(), "identity-service", "user", resolution.user().getId(), null, new UserUpdatedPayload(
							resolution.user().getId(), resolution.user().getEmail(), "social-account-linked")));
		}
	}

	private record SocialResolution(UserEntity user, boolean createdUser, boolean linkedExistingUser) {
	}
}
