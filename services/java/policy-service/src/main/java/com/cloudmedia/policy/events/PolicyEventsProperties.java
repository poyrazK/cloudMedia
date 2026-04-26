package com.cloudmedia.policy.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudmedia.policy.events")
public class PolicyEventsProperties {

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

		private String policyChanged = "cloudmedia.policy.changed";

		public String getPolicyChanged() {
			return policyChanged;
		}

		public void setPolicyChanged(String policyChanged) {
			this.policyChanged = policyChanged;
		}
	}
}
