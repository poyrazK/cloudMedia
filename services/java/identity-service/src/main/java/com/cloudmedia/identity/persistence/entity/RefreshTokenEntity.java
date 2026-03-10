package com.cloudmedia.identity.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

	@Id
	@Column(nullable = false, length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private SessionEntity session;

	@Column(name = "token_hash", nullable = false, length = 128)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_token_id")
	private RefreshTokenEntity parentToken;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "replaced_by")
	private RefreshTokenEntity replacedBy;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public SessionEntity getSession() {
		return session;
	}

	public void setSession(SessionEntity session) {
		this.session = session;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public RefreshTokenEntity getParentToken() {
		return parentToken;
	}

	public void setParentToken(RefreshTokenEntity parentToken) {
		this.parentToken = parentToken;
	}

	public LocalDateTime getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(LocalDateTime issuedAt) {
		this.issuedAt = issuedAt;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public LocalDateTime getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(LocalDateTime revokedAt) {
		this.revokedAt = revokedAt;
	}

	public RefreshTokenEntity getReplacedBy() {
		return replacedBy;
	}

	public void setReplacedBy(RefreshTokenEntity replacedBy) {
		this.replacedBy = replacedBy;
	}
}
