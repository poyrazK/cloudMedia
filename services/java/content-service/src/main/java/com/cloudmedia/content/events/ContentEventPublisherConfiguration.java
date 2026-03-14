package com.cloudmedia.content.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableConfigurationProperties(ContentEventsKafkaProperties.class)
public class ContentEventPublisherConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "cloudmedia.events.kafka", name = "enabled", havingValue = "true")
	KafkaContentEventPublisher kafkaContentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
			ContentEventsKafkaProperties properties) {
		return new KafkaContentEventPublisher(kafkaTemplate, properties);
	}

	@Bean
	@ConditionalOnMissingBean(ContentEventPublisher.class)
	NoopContentEventPublisher noopContentEventPublisher() {
		return new NoopContentEventPublisher();
	}
}
