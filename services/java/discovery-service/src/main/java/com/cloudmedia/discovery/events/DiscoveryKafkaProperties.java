package com.cloudmedia.discovery.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudmedia.discovery.kafka")
public class DiscoveryKafkaProperties {

	private boolean enabled;

	private String groupId = "discovery-indexer-v1";

	private final Topics topics = new Topics();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public Topics getTopics() {
		return topics;
	}

	public static class Topics {

		private String contentPublished = "cloudmedia.content.published";

		private String contentUpdated = "cloudmedia.content.updated";

		private String contentUnpublished = "cloudmedia.content.unpublished";

		public String getContentPublished() {
			return contentPublished;
		}

		public void setContentPublished(String contentPublished) {
			this.contentPublished = contentPublished;
		}

		public String getContentUpdated() {
			return contentUpdated;
		}

		public void setContentUpdated(String contentUpdated) {
			this.contentUpdated = contentUpdated;
		}

		public String getContentUnpublished() {
			return contentUnpublished;
		}

		public void setContentUnpublished(String contentUnpublished) {
			this.contentUnpublished = contentUnpublished;
		}
	}
}
