package com.cloudmedia.discovery.events;

import com.cloudmedia.discovery.search.NoopSearchIndexWriter;
import com.cloudmedia.discovery.search.OpenSearchSearchIndexWriter;
import com.cloudmedia.discovery.search.SearchIndexWriter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryIndexBridgeConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(DiscoveryIndexBridgeConfiguration.class)
			.withBean(RestTemplateBuilder.class, RestTemplateBuilder::new);

	@Test
	void usesNoopWriterWhenOpenSearchIsDisabled() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(SearchIndexWriter.class);
			assertThat(context).hasSingleBean(NoopSearchIndexWriter.class);
		});
	}

	@Test
	void createsOpenSearchWriterWhenEnabled() {
		contextRunner.withPropertyValues("cloudmedia.discovery.opensearch.enabled=true").run(context -> {
			assertThat(context).hasSingleBean(SearchIndexWriter.class);
			assertThat(context).hasSingleBean(OpenSearchSearchIndexWriter.class);
			assertThat(context).doesNotHaveBean(NoopSearchIndexWriter.class);
		});
	}

	@Test
	void doesNotCreateListenerWhenKafkaBridgeIsDisabled() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ContentIndexEventListener.class));
	}
}
