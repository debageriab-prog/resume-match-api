package se.debageri.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
import se.debageri.api.entity.ResumeMatch;
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

	@Test
	void shouldReturnStatistics_whenNoAssignmentsExist() {
		// Given — empty database

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/statistics", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("todayCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("lastWeekCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("lastMonthCount").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnStatistics_withCountsReflectingSeededData() {
		// Given — assignments published today count toward all period buckets
		Assignment a1 = buildAssignment(11001L, "Dev A", "CorpA", "p1");
		a1.setPublishedOn(LocalDate.now());
		Assignment a2 = buildAssignment(11002L, "Dev B", "CorpB", "p2");
		a2.setPublishedOn(LocalDate.now());
		assignmentRepository.save(a1);
		assignmentRepository.save(a2);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/statistics", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("todayCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("lastWeekCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("lastMonthCount").asLong()).isEqualTo(2);
	}

	@Test
	void shouldReturnEmptyList_whenNoMatchedAssignmentsExist() {
		// Given — assignments exist but no matches

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void shouldReturnOnlyAssignmentsWithPositiveDecisionMatches() {
		// Given
		Assignment matched = buildAssignment(20001L, "Java Dev", "CorpA", "p1");
		matched.setPublishedOn(LocalDate.now());
		matched = assignmentRepository.save(matched);

		Assignment noMatches = buildAssignment(20002L, "Python Dev", "CorpB", "p2");
		noMatches.setPublishedOn(LocalDate.now());
		noMatches = assignmentRepository.save(noMatches);

		// A match with decision "yes"
		ResumeMatch yesMatch = new ResumeMatch();
		yesMatch.setResumeId(1L);
		yesMatch.setAssignmentId(matched.getId());
		yesMatch.setMatchPercent(80);
		yesMatch.setScore(0.8);
		yesMatch.setDecision("yes");
		resumeMatchRepository.save(yesMatch);

		// A match with decision "no" (should be excluded)
		ResumeMatch noMatch = new ResumeMatch();
		noMatch.setResumeId(2L);
		noMatch.setAssignmentId(noMatches.getId());
		noMatch.setMatchPercent(30);
		noMatch.setScore(0.3);
		noMatch.setDecision("no");
		resumeMatchRepository.save(noMatch);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(1);
		assertThat(response.getBody().get(0).get("id").asLong()).isEqualTo(matched.getId());
		assertThat(response.getBody().get(0).get("matchCount").asLong()).isEqualTo(1);
	}

	@Test
	void shouldExcludeAssignmentsWithNullDecisionMatches() {
		// Given
		Assignment assignment = buildAssignment(20010L, "Dev", "Corp", "p1");
		assignment.setPublishedOn(LocalDate.now());
		assignment = assignmentRepository.save(assignment);

		// Match with null decision (no judge decision yet)
		ResumeMatch nullDecision = new ResumeMatch();
		nullDecision.setResumeId(1L);
		nullDecision.setAssignmentId(assignment.getId());
		nullDecision.setMatchPercent(70);
		nullDecision.setScore(0.7);
		// decision remains null
		resumeMatchRepository.save(nullDecision);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void shouldCountAllPositiveDecisionsPerAssignment() {
		// Given — assignment with multiple positive decision matches
		Assignment assignment = buildAssignment(20020L, "Full Stack Dev", "TechCorp", "p1");
		assignment.setPublishedOn(LocalDate.now());
		assignment = assignmentRepository.save(assignment);

		for (String decision : new String[]{"yes", "maybe", "strong_yes"}) {
			ResumeMatch m = new ResumeMatch();
			m.setResumeId((long) decision.hashCode());
			m.setAssignmentId(assignment.getId());
			m.setMatchPercent(75);
			m.setScore(0.75);
			m.setDecision(decision);
			resumeMatchRepository.save(m);
		}

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(1);
		assertThat(response.getBody().get(0).get("matchCount").asLong()).isEqualTo(3);
	}

	@Test
	void shouldReturnAtMostFiveAssignments() {
		// Given — 7 assignments each with a positive decision match
		for (int i = 0; i < 7; i++) {
			Assignment a = buildAssignment(21000L + i, "Role " + i, "Corp", "p1");
			a.setPublishedOn(LocalDate.now().minusDays(i));
			a = assignmentRepository.save(a);

			ResumeMatch m = new ResumeMatch();
			m.setResumeId((long) i + 1);
			m.setAssignmentId(a.getId());
			m.setMatchPercent(70);
			m.setScore(0.7);
			m.setDecision("yes");
			resumeMatchRepository.save(m);
		}

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/assignments/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(5);
	}
}
