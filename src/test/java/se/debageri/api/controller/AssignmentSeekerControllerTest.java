package se.debageri.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.client.OpenAIClient;

import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.rabbit.AssignmentEventPublisher;
import se.debageri.api.repository.AssignmentSeekerRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AssignmentSeekerControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private AssignmentSeekerRepository seekerRepository;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private ResumeMatchRepository resumeMatchRepository;

	@MockBean
	private OpenAIClient openAIClient;

	@MockBean
	private AssignmentEventPublisher assignmentEventPublisher;

	@BeforeEach
	void setUp() {
		resumeMatchRepository.deleteAll();
		resumeRepository.deleteAll();
		seekerRepository.deleteAll();
	}

	private AssignmentSeeker buildSeeker(String firstName, String lastName, String email) {
		AssignmentSeeker seeker = new AssignmentSeeker();
		seeker.setFirstName(firstName);
		seeker.setLastName(lastName);
		seeker.setEmail(email);
		return seeker;
	}

	@Test
	void shouldReturnEmptyPage_whenNoSeekersExist() {
		// Given — empty database

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignment-seekers", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("content").isArray()).isTrue();
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnAllSeekers_whenTheyExist() {
		// Given
		seekerRepository.save(buildSeeker("Alice", "Smith", "alice@example.com"));
		seekerRepository.save(buildSeeker("Bob", "Jones", "bob@example.com"));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignment-seekers", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldReturnSeeker_whenItExists() {
		// Given
		AssignmentSeeker saved = seekerRepository.save(buildSeeker("Carol", "White", "carol@example.com"));

		// When
		ResponseEntity<AssignmentSeeker> response = restTemplate
				.getForEntity("/api/assignment-seekers/" + saved.getId(), AssignmentSeeker.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getEmail()).isEqualTo("carol@example.com");
		assertThat(response.getBody().getFirstName()).isEqualTo("Carol");
	}

	@Test
	void shouldReturn404_whenSeekerDoesNotExist() {
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignment-seekers/" + nonExistentId,
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().get("message").asText()).contains("9999");
	}

	@Test
	void shouldCreateSeeker_whenValidDataIsProvided() {
		// Given
		AssignmentSeeker seeker = buildSeeker("Dave", "Brown", "dave@example.com");

		// When
		ResponseEntity<AssignmentSeeker> response = restTemplate.postForEntity("/api/assignment-seekers", seeker,
				AssignmentSeeker.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody().getId()).isNotNull();
		assertThat(response.getBody().getEmail()).isEqualTo("dave@example.com");
		assertThat(seekerRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldReturn409_whenEmailAlreadyExists() {
		// Given
		seekerRepository.save(buildSeeker("Eve", "Green", "eve@example.com"));
		AssignmentSeeker duplicate = buildSeeker("Eve2", "Green2", "eve@example.com");

		// When
		ResponseEntity<JsonNode> response = restTemplate.postForEntity("/api/assignment-seekers", duplicate,
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void shouldUpdateSeeker_whenItExists() {
		// Given
		AssignmentSeeker existing = seekerRepository.save(buildSeeker("Frank", "Old", "frank@example.com"));
		AssignmentSeeker updated = buildSeeker("Frank", "New", "frank.new@example.com");

		// When
		ResponseEntity<AssignmentSeeker> response = restTemplate.exchange("/api/assignment-seekers/" + existing.getId(),
				HttpMethod.PUT, new HttpEntity<>(updated), AssignmentSeeker.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getLastName()).isEqualTo("New");
		assertThat(response.getBody().getEmail()).isEqualTo("frank.new@example.com");
	}

	@Test
	void shouldReturn404_whenUpdatingNonExistentSeeker() {
		// Given
		AssignmentSeeker payload = buildSeeker("Ghost", "User", "ghost@example.com");

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/assignment-seekers/9999", HttpMethod.PUT,
				new HttpEntity<>(payload), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturn409_whenUpdatingSeekerWithAlreadyUsedEmail() {
		// Given
		AssignmentSeeker first = seekerRepository.save(buildSeeker("Alice", "A", "alice.a@example.com"));
		AssignmentSeeker second = seekerRepository.save(buildSeeker("Bob", "B", "bob.b@example.com"));
		AssignmentSeeker updateRequest = buildSeeker("Bob", "B", "alice.a@example.com");

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/assignment-seekers/" + second.getId(),
				HttpMethod.PUT, new HttpEntity<>(updateRequest), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void shouldDeleteSeeker_whenItExists() {
		// Given
		AssignmentSeeker saved = seekerRepository.save(buildSeeker("Henry", "H", "henry@example.com"));

		// When
		ResponseEntity<Void> response = restTemplate.exchange("/api/assignment-seekers/" + saved.getId(),
				HttpMethod.DELETE, null, Void.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(seekerRepository.existsById(saved.getId())).isFalse();
	}

	@Test
	void shouldReturn404_whenDeletingNonExistentSeeker() {
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/assignment-seekers/" + nonExistentId,
				HttpMethod.DELETE, null, JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
