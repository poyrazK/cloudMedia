package com.cloudmedia.content.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudmedia.content.events")
public class ContentEventsProperties {

	private boolean enabled;

	private final Topics topics = new Topics();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Topics getTopics() {
		return topics;
	}

	public static class Topics {

		private String contentPublished = "cloudmedia.content.published";

		public String getContentPublished() {
			return contentPublished;
		}

		public void setContentPublished(String contentPublished) {
			this.contentPublished = contentPublished;
		}
	}
}
