package com.cloudmedia.content.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudmedia.events.kafka")
public class ContentEventsKafkaProperties {

	private boolean enabled;

	private String topicPrefix = "cloudmedia.content";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getTopicPrefix() {
		return topicPrefix;
	}

	public void setTopicPrefix(String topicPrefix) {
		this.topicPrefix = topicPrefix;
	}
}
