package com.cloudmedia.policy.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableConfigurationProperties(PolicyEventsProperties.class)
public class PolicyEventsConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "cloudmedia.policy.events", name = "enabled", havingValue = "true")
	KafkaPolicyEventPublisher kafkaPolicyEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
			PolicyEventsProperties properties) {
		return new KafkaPolicyEventPublisher(kafkaTemplate, properties);
	}

	@Bean
	@ConditionalOnMissingBean(PolicyEventPublisher.class)
	NoopPolicyEventPublisher noopPolicyEventPublisher() {
		return new NoopPolicyEventPublisher();
	}
}
