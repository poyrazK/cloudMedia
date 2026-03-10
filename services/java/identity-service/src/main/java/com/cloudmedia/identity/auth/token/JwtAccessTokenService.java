package com.cloudmedia.identity.auth.token;

import com.cloudmedia.identity.auth.config.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenService {

	private final AuthProperties authProperties;

	public JwtAccessTokenService(AuthProperties authProperties) {
		this.authProperties = authProperties;
	}

	public String issueAccessToken(String userId, String sessionId, Instant now) {
		Instant expiresAt = now.plus(authProperties.getAccessTokenTtl());

		return Jwts.builder().subject(userId).issuer(authProperties.getJwtIssuer()).issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt)).claim("sid", sessionId).signWith(secretKey()).compact();
	}

	private SecretKey secretKey() {
		return Keys.hmacShaKeyFor(authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
	}
}
