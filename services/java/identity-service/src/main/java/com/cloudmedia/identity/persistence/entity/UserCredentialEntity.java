package com.cloudmedia.identity.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_credentials")
public class UserCredentialEntity {

	@Id
	@Column(name = "user_id", nullable = false, length = 36)
	private String userId;

	@OneToOne(optional = false)
	@MapsId
	@JoinColumn(name = "user_id")
	private UserEntity user;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public String getUserId() {
		return userId;
	}

	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
