package com.cloudmedia.policy.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "content_policy")
public class ContentPolicyEntity {

	@Id
	@Column(name = "content_id", nullable = false, length = 36)
	private String contentId;

	@Column(name = "age_restricted", nullable = false)
	private boolean ageRestricted;

	@Column(name = "geo_allow_list", nullable = false, length = 512)
	private String geoAllowList;

	@Column(name = "geo_block_list", nullable = false, length = 512)
	private String geoBlockList;

	@Enumerated(EnumType.STRING)
	@Column(name = "moderation_state", nullable = false, length = 16)
	private ModerationState moderationState;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public boolean isAgeRestricted() {
		return ageRestricted;
	}

	public void setAgeRestricted(boolean ageRestricted) {
		this.ageRestricted = ageRestricted;
	}

	public String getGeoAllowList() {
		return geoAllowList;
	}

	public void setGeoAllowList(String geoAllowList) {
		this.geoAllowList = geoAllowList;
	}

	public String getGeoBlockList() {
		return geoBlockList;
	}

	public void setGeoBlockList(String geoBlockList) {
		this.geoBlockList = geoBlockList;
	}

	public ModerationState getModerationState() {
		return moderationState;
	}

	public void setModerationState(ModerationState moderationState) {
		this.moderationState = moderationState;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		if (moderationState == null) {
			moderationState = ModerationState.VISIBLE;
		}
		if (geoAllowList == null) {
			geoAllowList = "";
		}
		if (geoBlockList == null) {
			geoBlockList = "";
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
