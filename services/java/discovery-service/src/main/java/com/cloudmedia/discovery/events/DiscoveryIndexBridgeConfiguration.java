package com.cloudmedia.discovery.events;

import com.cloudmedia.discovery.search.NoopSearchIndexWriter;
import com.cloudmedia.discovery.search.OpenSearchProperties;
import com.cloudmedia.discovery.search.OpenSearchSearchIndexWriter;
import com.cloudmedia.discovery.search.SearchIndexWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableKafka
@EnableConfigurationProperties({DiscoveryKafkaProperties.class, OpenSearchProperties.class})
public class DiscoveryIndexBridgeConfiguration {

	@Bean
	@ConditionalOnMissingBean(ObjectMapper.class)
	ObjectMapper objectMapper() {
		return new ObjectMapper().findAndRegisterModules();
	}

	@Bean
	@ConditionalOnProperty(prefix = "cloudmedia.discovery.opensearch", name = "enabled", havingValue = "true")
	OpenSearchSearchIndexWriter openSearchSearchIndexWriter(RestTemplateBuilder restTemplateBuilder,
			OpenSearchProperties openSearchProperties) {
		RestTemplate restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(30)).build();
		return new OpenSearchSearchIndexWriter(restTemplate, openSearchProperties);
	}

	@Bean
	@ConditionalOnMissingBean(SearchIndexWriter.class)
	NoopSearchIndexWriter noopSearchIndexWriter() {
		return new NoopSearchIndexWriter();
	}

	@Bean
	@ConditionalOnProperty(prefix = "cloudmedia.discovery.kafka", name = "enabled", havingValue = "true")
	ContentIndexEventListener contentIndexEventListener(ObjectMapper objectMapper,
			SearchIndexWriter searchIndexWriter) {
		return new ContentIndexEventListener(objectMapper, searchIndexWriter);
	}
}
