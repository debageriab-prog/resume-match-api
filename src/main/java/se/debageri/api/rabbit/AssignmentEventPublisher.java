package se.debageri.api.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AssignmentEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(AssignmentEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper mapper;

	public void publishAssignmentUpserted(long assignmentId) {
		try {
			String body = mapper.writeValueAsString(EventEnvelope.forAssignment("assignment.upserted", assignmentId));
			rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_EVENTS, "assignment.upserted", body);
			log.info("Published assignment.upserted assignmentId={}", assignmentId);
		} catch (Exception e) {
			log.warn("Failed to publish assignment.upserted assignmentId={} reason={}", assignmentId, e.getMessage());
		}
	}
}
