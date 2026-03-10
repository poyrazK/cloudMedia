package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.auth.config.AuthProperties;
import com.cloudmedia.identity.auth.token.JwtAccessTokenService;
import com.cloudmedia.identity.auth.token.RefreshTokenGenerator;
import com.cloudmedia.identity.auth.token.RefreshTokenHasher;
import com.cloudmedia.identity.error.ApiException;
import com.cloudmedia.identity.persistence.entity.RefreshTokenEntity;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.repository.RefreshTokenRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthRefreshService {

	private final AuthProperties authProperties;
	private final JwtAccessTokenService jwtAccessTokenService;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshTokenRepository refreshTokenRepository;
	private final SessionLifecycleService sessionLifecycleService;

	public AuthRefreshService(
			AuthProperties authProperties,
			JwtAccessTokenService jwtAccessTokenService,
			RefreshTokenGenerator refreshTokenGenerator,
			RefreshTokenHasher refreshTokenHasher,
			RefreshTokenRepository refreshTokenRepository,
			SessionLifecycleService sessionLifecycleService) {
		this.authProperties = authProperties;
		this.jwtAccessTokenService = jwtAccessTokenService;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.refreshTokenHasher = refreshTokenHasher;
		this.refreshTokenRepository = refreshTokenRepository;
		this.sessionLifecycleService = sessionLifecycleService;
	}

	@Transactional
	public RefreshResult rotateRefreshToken(String rawRefreshToken) {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		String tokenHash = refreshTokenHasher.hash(rawRefreshToken);

		RefreshTokenEntity currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> unauthorized("REFRESH_TOKEN_INVALID", "Refresh token is invalid"));

		if (currentToken.getRevokedAt() != null) {
			handleReuseDetection(currentToken, now);
			throw unauthorized("REFRESH_TOKEN_REUSED", "Refresh token reuse detected");
		}

		SessionEntity session = currentToken.getSession();
		if (session.getRevokedAt() != null || currentToken.getExpiresAt().isBefore(now) || session.getExpiresAt().isBefore(now)) {
			throw unauthorized("REFRESH_TOKEN_EXPIRED", "Refresh token is expired or revoked");
		}

		String newRawRefreshToken = refreshTokenGenerator.generate();
		RefreshTokenEntity newToken = new RefreshTokenEntity();
		newToken.setId(UUID.randomUUID().toString());
		newToken.setSession(session);
		newToken.setTokenHash(refreshTokenHasher.hash(newRawRefreshToken));
		newToken.setFamilyId(currentToken.getFamilyId());
		newToken.setParentToken(currentToken);
		newToken.setIssuedAt(now);
		newToken.setExpiresAt(now.plus(authProperties.getRefreshTokenTtl()));

		refreshTokenRepository.save(newToken);

		currentToken.setRevokedAt(now);
		currentToken.setReplacedBy(newToken);
		refreshTokenRepository.save(currentToken);

		String accessToken = jwtAccessTokenService.issueAccessToken(session.getUser().getId(), session.getId(), Instant.now());
		return new RefreshResult(accessToken, newRawRefreshToken, session.getId());
	}

	private void handleReuseDetection(RefreshTokenEntity currentToken, LocalDateTime now) {
		List<RefreshTokenEntity> activeFamilyTokens = refreshTokenRepository.findByFamilyIdAndRevokedAtIsNull(currentToken.getFamilyId());
		for (RefreshTokenEntity token : activeFamilyTokens) {
			token.setRevokedAt(now);
		}
		refreshTokenRepository.saveAll(activeFamilyTokens);

		sessionLifecycleService.revokeSessionAndActiveTokens(currentToken.getSession(), now);
	}

	private ApiException unauthorized(String code, String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, code, message, null);
	}
}
