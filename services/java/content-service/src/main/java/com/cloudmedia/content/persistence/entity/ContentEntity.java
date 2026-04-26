package com.cloudmedia.content.persistence.entity;

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
@Table(name = "content")
public class ContentEntity {

	@Id
	@Column(nullable = false, length = 36)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "channel_id", nullable = false)
	private ChannelEntity channel;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(length = 4000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", nullable = false, length = 32)
	private ContentType contentType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ContentState state;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ContentVisibility visibility;

	@Column(name = "playback_ready", nullable = false)
	private boolean playbackReady;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "thumbnail_url", length = 512)
	private String thumbnailUrl;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ChannelEntity getChannel() {
		return channel;
	}

	public void setChannel(ChannelEntity channel) {
		this.channel = channel;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ContentType getContentType() {
		return contentType;
	}

	public void setContentType(ContentType contentType) {
		this.contentType = contentType;
	}

	public ContentState getState() {
		return state;
	}

	public void setState(ContentState state) {
		this.state = state;
	}

	public ContentVisibility getVisibility() {
		return visibility;
	}

	public void setVisibility(ContentVisibility visibility) {
		this.visibility = visibility;
	}

	public boolean isPlaybackReady() {
		return playbackReady;
	}

	public void setPlaybackReady(boolean playbackReady) {
		this.playbackReady = playbackReady;
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

	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(LocalDateTime publishedAt) {
		this.publishedAt = publishedAt;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}
}
