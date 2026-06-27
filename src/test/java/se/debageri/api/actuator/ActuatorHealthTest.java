package se.debageri.api.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.client.OpenAIClient;

import se.debageri.api.rabbit.AssignmentEventPublisher;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorHealthTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@MockBean
	private OpenAIClient openAIClient;

	@MockBean
	private AssignmentEventPublisher assignmentEventPublisher;

	@Test
	void healthEndpointReturns200() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void healthEndpointReturnsStatusUp() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().get("status").asText()).isEqualTo("UP");
	}

	@Test
	void healthEndpointIncludesComponentDetails() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().has("components")).isTrue();
	}

	@Test
	void healthEndpointIncludesDiskSpaceComponent() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

		assertThat(response.getBody()).isNotNull();
		JsonNode components = response.getBody().get("components");
		assertThat(components.has("diskSpace")).isTrue();
		assertThat(components.get("diskSpace").get("status").asText()).isEqualTo("UP");
	}

	@Test
	void healthEndpointIncludesPingComponent() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

		assertThat(response.getBody()).isNotNull();
		JsonNode components = response.getBody().get("components");
		assertThat(components.has("ping")).isTrue();
		assertThat(components.get("ping").get("status").asText()).isEqualTo("UP");
	}

	@Test
	void onlyHealthEndpointIsExposed() {
		ResponseEntity<JsonNode> infoResponse = restTemplate.getForEntity("/actuator/info", JsonNode.class);
		ResponseEntity<JsonNode> metricsResponse = restTemplate.getForEntity("/actuator/metrics", JsonNode.class);

		assertThat(infoResponse.getStatusCode().is2xxSuccessful()).isFalse();
		assertThat(metricsResponse.getStatusCode().is2xxSuccessful()).isFalse();
	}
}
