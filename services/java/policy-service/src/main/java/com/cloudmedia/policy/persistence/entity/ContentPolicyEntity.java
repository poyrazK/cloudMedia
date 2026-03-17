package com.cloudmedia.policy.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

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

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
