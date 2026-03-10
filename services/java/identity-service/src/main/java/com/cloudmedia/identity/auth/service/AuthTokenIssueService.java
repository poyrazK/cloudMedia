package com.cloudmedia.identity.auth.service;

import com.cloudmedia.identity.auth.config.AuthProperties;
import com.cloudmedia.identity.auth.token.JwtAccessTokenService;
import com.cloudmedia.identity.auth.token.RefreshTokenGenerator;
import com.cloudmedia.identity.auth.token.RefreshTokenHasher;
import com.cloudmedia.identity.persistence.entity.RefreshTokenEntity;
import com.cloudmedia.identity.persistence.entity.SessionEntity;
import com.cloudmedia.identity.persistence.repository.RefreshTokenRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenIssueService {

	private final AuthProperties authProperties;
	private final JwtAccessTokenService jwtAccessTokenService;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshTokenRepository refreshTokenRepository;

	public AuthTokenIssueService(AuthProperties authProperties, JwtAccessTokenService jwtAccessTokenService,
			RefreshTokenGenerator refreshTokenGenerator, RefreshTokenHasher refreshTokenHasher,
			RefreshTokenRepository refreshTokenRepository) {
		this.authProperties = authProperties;
		this.jwtAccessTokenService = jwtAccessTokenService;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.refreshTokenHasher = refreshTokenHasher;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	@Transactional
	public RefreshResult issueForSession(SessionEntity session) {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		String rawRefreshToken = refreshTokenGenerator.generate();

		RefreshTokenEntity refreshToken = new RefreshTokenEntity();
		refreshToken.setId(UUID.randomUUID().toString());
		refreshToken.setSession(session);
		refreshToken.setTokenHash(refreshTokenHasher.hash(rawRefreshToken));
		refreshToken.setFamilyId(refreshToken.getId());
		refreshToken.setIssuedAt(now);
		refreshToken.setExpiresAt(now.plus(authProperties.getRefreshTokenTtl()));

		refreshTokenRepository.save(refreshToken);

		String accessToken = jwtAccessTokenService.issueAccessToken(session.getUser().getId(), session.getId(),
				Instant.now());
		return new RefreshResult(accessToken, rawRefreshToken, session.getId());
	}
}
