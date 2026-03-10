package com.cloudmedia.identity.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

	private String jwtIssuer = "cloudmedia-identity";

	private String jwtSecret = "change-me-in-production-change-me-in-production";

	private Duration accessTokenTtl = Duration.ofMinutes(15);

	private Duration refreshTokenTtl = Duration.ofDays(30);

	private int maxActiveSessions = 5;

	public String getJwtIssuer() {
		return jwtIssuer;
	}

	public void setJwtIssuer(String jwtIssuer) {
		this.jwtIssuer = jwtIssuer;
	}

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public Duration getAccessTokenTtl() {
		return accessTokenTtl;
	}

	public void setAccessTokenTtl(Duration accessTokenTtl) {
		this.accessTokenTtl = accessTokenTtl;
	}

	public Duration getRefreshTokenTtl() {
		return refreshTokenTtl;
	}

	public void setRefreshTokenTtl(Duration refreshTokenTtl) {
		this.refreshTokenTtl = refreshTokenTtl;
	}

	public int getMaxActiveSessions() {
		return maxActiveSessions;
	}

	public void setMaxActiveSessions(int maxActiveSessions) {
		this.maxActiveSessions = maxActiveSessions;
	}
}
