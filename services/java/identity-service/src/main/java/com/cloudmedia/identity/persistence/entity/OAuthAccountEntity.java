package com.cloudmedia.identity.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_accounts")
public class OAuthAccountEntity {

	@Id
	@Column(nullable = false, length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OAuthProvider provider;

	@Column(name = "provider_subject", nullable = false, length = 255)
	private String providerSubject;

	@Column(name = "linked_at", nullable = false)
	private LocalDateTime linkedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public OAuthProvider getProvider() {
		return provider;
	}

	public void setProvider(OAuthProvider provider) {
		this.provider = provider;
	}

	public String getProviderSubject() {
		return providerSubject;
	}

	public void setProviderSubject(String providerSubject) {
		this.providerSubject = providerSubject;
	}

	public LocalDateTime getLinkedAt() {
		return linkedAt;
	}

	public void setLinkedAt(LocalDateTime linkedAt) {
		this.linkedAt = linkedAt;
	}
}
