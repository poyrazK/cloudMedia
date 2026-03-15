package com.cloudmedia.discovery.events;

import com.cloudmedia.discovery.search.NoopSearchIndexWriter;
import com.cloudmedia.discovery.search.NoopSearchIndexReader;
import com.cloudmedia.discovery.search.OpenSearchSearchIndexWriter;
import com.cloudmedia.discovery.search.OpenSearchSearchIndexReader;
import com.cloudmedia.discovery.search.SearchIndexReader;
import com.cloudmedia.discovery.search.SearchIndexWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

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
			assertThat(context).hasSingleBean(SearchIndexReader.class);
			assertThat(context).hasSingleBean(NoopSearchIndexReader.class);
		});
	}

	@Test
	void createsOpenSearchWriterWhenEnabled() {
		contextRunner.withPropertyValues("cloudmedia.discovery.opensearch.enabled=true").run(context -> {
			assertThat(context).hasSingleBean(SearchIndexWriter.class);
			assertThat(context).hasSingleBean(OpenSearchSearchIndexWriter.class);
			assertThat(context).doesNotHaveBean(NoopSearchIndexWriter.class);
			assertThat(context).hasSingleBean(SearchIndexReader.class);
			assertThat(context).hasSingleBean(OpenSearchSearchIndexReader.class);
			assertThat(context).doesNotHaveBean(NoopSearchIndexReader.class);
		});
	}

	@Test
	void doesNotCreateListenerWhenKafkaBridgeIsDisabled() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ContentIndexEventListener.class));
	}

	@Test
	void createsListenerWhenKafkaBridgeIsEnabled() {
		contextRunner.withPropertyValues("cloudmedia.discovery.kafka.enabled=true")
				.withBean(ObjectMapper.class, ObjectMapper::new)
				.withBean("kafkaListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class, () -> {
					ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
					factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(
							Map.of("bootstrap.servers", "localhost:9092", "group.id", "test-group", "key.deserializer",
									"org.apache.kafka.common.serialization.StringDeserializer", "value.deserializer",
									"org.apache.kafka.common.serialization.StringDeserializer")));
					factory.setAutoStartup(false);
					return factory;
				}).run(context -> assertThat(context).hasSingleBean(ContentIndexEventListener.class));
	}
}
