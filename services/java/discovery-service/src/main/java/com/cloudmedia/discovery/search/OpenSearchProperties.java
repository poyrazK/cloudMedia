package com.cloudmedia.discovery.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudmedia.discovery.opensearch")
public class OpenSearchProperties {

	private boolean enabled;

	private String baseUrl = "http://localhost:9200";

	private String indexAlias = "content-read";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getIndexAlias() {
		return indexAlias;
	}

	public void setIndexAlias(String indexAlias) {
		this.indexAlias = indexAlias;
	}
}
