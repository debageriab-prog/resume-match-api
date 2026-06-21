package se.debageri.api.rabbit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope {

	public String event_id;
	public String type;
	public String occurred_at;
	public String source;
	public int version;
	public Map<String, Object> payload;

	/** Creates a minimal event envelope with an assignment_id payload. */
	public static EventEnvelope forAssignment(String type, long assignmentId) {
		EventEnvelope e = new EventEnvelope();
		e.event_id = UUID.randomUUID().toString();
		e.type = type;
		e.occurred_at = Instant.now().toString();
		e.source = "resume-match-api";
		e.version = 1;
		e.payload = Map.of("assignment_id", assignmentId);
		return e;
	}
}
