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

import se.debageri.api.entity.Assignment;
import se.debageri.api.rabbit.AssignmentEventPublisher;
import se.debageri.api.repository.AssignmentIndexRepository;
import se.debageri.api.repository.AssignmentRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.service.ElasticJobSearchService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AssignmentControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private AssignmentRepository assignmentRepository;

	@Autowired
	private AssignmentIndexRepository assignmentIndexRepository;

	@Autowired
	private ResumeMatchRepository resumeMatchRepository;

	@MockBean
	private OpenAIClient openAIClient;

	@MockBean
	private AssignmentEventPublisher assignmentEventPublisher;

	@MockBean
	private ElasticJobSearchService elasticJobSearchService;

	@BeforeEach
	void setUp() {
		resumeMatchRepository.deleteAll();
		assignmentIndexRepository.deleteAll();
		assignmentRepository.deleteAll();
	}

	private Assignment buildAssignment(long jobId, String title, String client, String portal) {
		Assignment a = new Assignment();
		a.setJobId(jobId);
		a.setTitle(title);
		a.setClient(client);
		a.setPortal(portal);
		return a;
	}

	@Test
	void shouldReturnEmptyPage_whenNoAssignmentsExist() {
		// Given — empty database

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("content").isArray()).isTrue();
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnAllAssignments_whenTheyExist() {
		// Given
		assignmentRepository.save(buildAssignment(1001L, "Java Developer", "Acme Corp", "linkedin"));
		assignmentRepository.save(buildAssignment(1002L, "React Developer", "Tech Inc", "indeed"));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldReturnAssignment_whenItExists() {
		// Given
		Assignment saved = assignmentRepository
				.save(buildAssignment(2001L, "Backend Engineer", "StartupX", "glassdoor"));

		// When
		ResponseEntity<Assignment> response = restTemplate.getForEntity("/api/assignments/" + saved.getId(),
				Assignment.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getJobId()).isEqualTo(2001L);
		assertThat(response.getBody().getTitle()).isEqualTo("Backend Engineer");
	}

	@Test
	void shouldReturn404_whenAssignmentDoesNotExist() {
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/" + nonExistentId,
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().get("message").asText()).contains("9999");
	}

	@Test
	void shouldCreateAssignment_whenValidDataIsProvided() {
		// Given
		Assignment assignment = buildAssignment(3001L, "DevOps Engineer", "CloudCo", "stackoverflow");

		// When
		ResponseEntity<Assignment> response = restTemplate.postForEntity("/api/assignments", assignment,
				Assignment.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody().getId()).isNotNull();
		assertThat(response.getBody().getJobId()).isEqualTo(3001L);
		assertThat(assignmentRepository.count()).isEqualTo(1);
	}

	@Test
	void shouldReturn409_whenDuplicateJobIdIsSubmitted() {
		// Given
		assignmentRepository.save(buildAssignment(4001L, "QA Engineer", "TestCorp", "monster"));
		Assignment duplicate = buildAssignment(4001L, "QA Engineer Copy", "TestCorp", "monster");

		// When
		ResponseEntity<JsonNode> response = restTemplate.postForEntity("/api/assignments", duplicate, JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void shouldUpdateAssignment_whenItExists() {
		// Given
		Assignment existing = assignmentRepository.save(buildAssignment(5001L, "Old Title", "OldCorp", "linkedin"));
		Assignment updated = buildAssignment(5001L, "New Title", "NewCorp", "indeed");

		// When
		ResponseEntity<Assignment> response = restTemplate.exchange("/api/assignments/" + existing.getId(),
				HttpMethod.PUT, new HttpEntity<>(updated), Assignment.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getTitle()).isEqualTo("New Title");
		assertThat(response.getBody().getClient()).isEqualTo("NewCorp");
	}

	@Test
	void shouldReturn404_whenUpdatingNonExistentAssignment() {
		// Given
		Assignment payload = buildAssignment(6001L, "Title", "Corp", "portal");

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/assignments/9999", HttpMethod.PUT,
				new HttpEntity<>(payload), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldDeleteAssignment_whenItExists() {
		// Given
		Assignment saved = assignmentRepository.save(buildAssignment(7001L, "To Delete", "Corp", "portal"));

		// When
		ResponseEntity<Void> response = restTemplate.exchange("/api/assignments/" + saved.getId(), HttpMethod.DELETE,
				null, Void.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(assignmentRepository.existsById(saved.getId())).isFalse();
	}

	@Test
	void shouldReturn404_whenDeletingNonExistentAssignment() {
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/assignments/" + nonExistentId,
				HttpMethod.DELETE, null, JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldFilterAssignments_byTitlePartialMatch() {
		// Given
		assignmentRepository.save(buildAssignment(8001L, "Java Developer", "CorpA", "p1"));
		assignmentRepository.save(buildAssignment(8002L, "Python Developer", "CorpB", "p2"));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments?title=Java", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("title").asText()).isEqualTo("Java Developer");
	}

	@Test
	void shouldFilterAssignments_byExactPortal() {
		// Given
		assignmentRepository.save(buildAssignment(9001L, "Dev", "Corp", "linkedin"));
		assignmentRepository.save(buildAssignment(9002L, "Dev2", "Corp", "indeed"));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments?portal=linkedin",
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
	}

	@Test
	void shouldFilterAssignments_byJobId() {
		// Given
		assignmentRepository.save(buildAssignment(10001L, "Dev A", "CorpA", "p1"));
		assignmentRepository.save(buildAssignment(10002L, "Dev B", "CorpB", "p2"));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments?jobId=10001", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("jobId").asLong()).isEqualTo(10001L);
	}
}
