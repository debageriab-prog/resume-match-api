package se.debageri.api.rabbit;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String EXCHANGE_EVENTS = "events.topic";

	@Bean
	public TopicExchange eventsExchange() {
		return new TopicExchange(EXCHANGE_EVENTS, true, false);
	}
}
