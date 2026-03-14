package com.cloudmedia.content.events;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class ContentEventPublisherConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ContentEventPublisherConfiguration.class);

	@Test
	void usesNoopPublisherWhenKafkaIsDisabled() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ContentEventPublisher.class);
			assertThat(context).hasSingleBean(NoopContentEventPublisher.class);
		});
	}

	@Test
	void usesKafkaPublisherWhenKafkaIsEnabled() {
		contextRunner.withPropertyValues("cloudmedia.events.kafka.enabled=true")
				.withBean("kafkaTemplate", KafkaTemplate.class, KafkaContentEventPublisherTest.TestKafkaTemplate::new)
				.run(context -> {
					assertThat(context).hasSingleBean(ContentEventPublisher.class);
					assertThat(context).hasSingleBean(KafkaContentEventPublisher.class);
					assertThat(context).doesNotHaveBean(NoopContentEventPublisher.class);
				});
	}
}
