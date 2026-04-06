package com.cloudmedia.content.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableConfigurationProperties(ContentEventsProperties.class)
public class ContentEventsConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "cloudmedia.content.events", name = "enabled", havingValue = "true")
	KafkaContentEventPublisher kafkaContentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
			ContentEventsProperties properties) {
		return new KafkaContentEventPublisher(kafkaTemplate, properties);
	}

	@Bean
	@ConditionalOnMissingBean(ContentEventPublisher.class)
	NoopContentEventPublisher noopContentEventPublisher() {
		return new NoopContentEventPublisher();
	}
}
