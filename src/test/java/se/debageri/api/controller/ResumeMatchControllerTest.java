package se.debageri.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.client.OpenAIClient;

import se.debageri.api.entity.Assignment;
import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.NotificationType;
import se.debageri.api.entity.Resume;
import se.debageri.api.entity.ResumeMatch;
import se.debageri.api.rabbit.AssignmentEventPublisher;
import se.debageri.api.repository.AssignmentIndexRepository;
import se.debageri.api.repository.AssignmentRepository;
import se.debageri.api.repository.AssignmentSeekerRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ResumeMatchControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ResumeMatchRepository resumeMatchRepository;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private AssignmentSeekerRepository seekerRepository;

	@Autowired
	private AssignmentRepository assignmentRepository;

	@Autowired
	private AssignmentIndexRepository assignmentIndexRepository;

	@MockBean
	private OpenAIClient openAIClient;

	@MockBean
	private AssignmentEventPublisher assignmentEventPublisher;

	private AssignmentSeeker savedSeeker;
	private Resume savedResume;
	private Assignment savedAssignment;

	@BeforeEach
	void setUp() {
		resumeMatchRepository.deleteAll();
		assignmentIndexRepository.deleteAll();
		resumeRepository.deleteAll();
		seekerRepository.deleteAll();
		assignmentRepository.deleteAll();

		AssignmentSeeker seeker = new AssignmentSeeker();
		seeker.setFirstName("Test");
		seeker.setLastName("Seeker");
		seeker.setEmail("match.seeker@example.com");
		savedSeeker = seekerRepository.save(seeker);

		Resume resume = new Resume();
		resume.setOwner(savedSeeker);
		resume.setFileName("match-test.pdf");
		resume.setContentType("application/pdf");
		resume.setNotificationType(NotificationType.User);
		resume.setPdfBytes(new byte[]{});
		savedResume = resumeRepository.save(resume);

		Assignment assignment = new Assignment();
		assignment.setJobId(50001L);
		assignment.setTitle("Test Role");
		assignment.setClient("MatchCorp");
		savedAssignment = assignmentRepository.save(assignment);
	}

	private ResumeMatch buildMatch(long resumeId, long assignmentId, int matchPercent, double score) {
		ResumeMatch match = new ResumeMatch();
		match.setResumeId(resumeId);
		match.setAssignmentId(assignmentId);
		match.setMatchPercent(matchPercent);
		match.setScore(score);
		return match;
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches (with enriched response shape)
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldReturnEmptyPage_whenNoMatchesExist() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("content").isArray()).isTrue();
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnAllMatches_whenTheyExist() {
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 85, 0.85));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6));

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldReturnEnrichedAssignmentAndResume_inGetAll() {
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode item = response.getBody().get("content").get(0);

		// assignment object with id and title
		assertThat(item.get("assignment")).isNotNull();
		assertThat(item.get("assignment").get("id").asLong()).isEqualTo(savedAssignment.getId());
		assertThat(item.get("assignment").get("title").asText()).isEqualTo("Test Role");

		// resume object with id, fileName, ownerFullName
		assertThat(item.get("resume")).isNotNull();
		assertThat(item.get("resume").get("id").asLong()).isEqualTo(savedResume.getId());
		assertThat(item.get("resume").get("fileName").asText()).isEqualTo("match-test.pdf");
		assertThat(item.get("resume").get("ownerFullName").asText()).isEqualTo("Test Seeker");

		// raw id fields should NOT appear at the top level
		assertThat(item.has("assignmentId")).isFalse();
		assertThat(item.has("resumeId")).isFalse();
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches — filtering
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldFilterByAssignmentId() {
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6));

		ResponseEntity<JsonNode> response = restTemplate.getForEntity(
				"/api/resume-matches?assignmentId=" + savedAssignment.getId(), JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("assignment").get("id").asLong())
				.isEqualTo(savedAssignment.getId());
	}

	@Test
	void shouldFilterByResumeId() {
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 60, 0.6));

		ResponseEntity<JsonNode> response = restTemplate.getForEntity(
				"/api/resume-matches?resumeId=" + savedResume.getId(), JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("resume").get("id").asLong())
				.isEqualTo(savedResume.getId());
	}

	@Test
	void shouldFilterByDecisionNotNull_true() {
		ResumeMatch withDecision = buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8);
		withDecision.setDecision("yes");
		resumeMatchRepository.save(withDecision);

		ResumeMatch withoutDecision = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6);
		resumeMatchRepository.save(withoutDecision);

		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches?decisionNotNull=true", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("decision").asText()).isEqualTo("yes");
	}

	@Test
	void shouldFilterByDecisionNotNull_false() {
		ResumeMatch withDecision = buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8);
		withDecision.setDecision("yes");
		resumeMatchRepository.save(withDecision);

		ResumeMatch withoutDecision = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6);
		resumeMatchRepository.save(withoutDecision);

		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches?decisionNotNull=false", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		// decision is null — Jackson may serialize as JSON null or omit the field
		JsonNode decisionNode = response.getBody().get("content").get(0).path("decision");
		assertThat(decisionNode.isNull() || decisionNode.isMissingNode()).isTrue();
	}

	@Test
	void shouldFilterByDecisionValue() {
		for (String dec : new String[]{"no", "maybe", "yes", "strong_yes"}) {
			ResumeMatch m = buildMatch(savedResume.getId(), savedAssignment.getId() + dec.length(), 70, 0.7);
			m.setDecision(dec);
			resumeMatchRepository.save(m);
		}

		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches?decision=strong_yes", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("decision").asText()).isEqualTo("strong_yes");
	}

	@Test
	void shouldReturn400_whenDecisionValueIsInvalid() {
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches?decision=invalid_value", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().get("message").asText()).contains("invalid_value");
	}

	@Test
	void shouldSupportCombinedFilters_assignmentIdAndDecision() {
		// match1: target assignment, decision=yes
		ResumeMatch match1 = buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8);
		match1.setDecision("yes");
		resumeMatchRepository.save(match1);

		// match2: target assignment, decision=no (same assignment, different decision — must use different resume)
		ResumeMatch match2 = buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 70, 0.7);
		match2.setDecision("no");
		resumeMatchRepository.save(match2);

		// match3: different assignment, decision=yes (should be excluded by assignmentId filter)
		ResumeMatch match3 = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6);
		match3.setDecision("yes");
		resumeMatchRepository.save(match3);

		ResponseEntity<JsonNode> response = restTemplate.getForEntity(
				"/api/resume-matches?assignmentId=" + savedAssignment.getId() + "&decision=yes", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("decision").asText()).isEqualTo("yes");
		assertThat(response.getBody().get("content").get(0).get("assignment").get("id").asLong())
				.isEqualTo(savedAssignment.getId());
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches/{id} (enriched response shape)
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldReturnMatch_whenItExists() {
		ResumeMatch saved = resumeMatchRepository
				.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 75, 0.75));

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/" + saved.getId(),
				JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("id").asLong()).isEqualTo(saved.getId());
		assertThat(response.getBody().get("matchPercent").asInt()).isEqualTo(75);
		assertThat(response.getBody().get("score").asDouble()).isEqualTo(0.75);
		assertThat(response.getBody().get("assignment").get("id").asLong()).isEqualTo(savedAssignment.getId());
		assertThat(response.getBody().get("assignment").get("title").asText()).isEqualTo("Test Role");
		assertThat(response.getBody().get("resume").get("id").asLong()).isEqualTo(savedResume.getId());
		assertThat(response.getBody().get("resume").get("fileName").asText()).isEqualTo("match-test.pdf");
		assertThat(response.getBody().get("resume").get("ownerFullName").asText()).isEqualTo("Test Seeker");
	}

	@Test
	void shouldReturn404_whenMatchDoesNotExist() {
		long nonExistentId = 9999L;

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/" + nonExistentId,
				JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().get("message").asText()).contains("9999");
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches/resume/{resumeId}
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldReturnMatchesByResumeId_sortedByMatchPercentDesc() {
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 50, 0.5));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 90, 0.9));
		resumeMatchRepository.save(buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 70, 0.7));

		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/resume/" + savedResume.getId(), JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
		int firstMatchPercent = response.getBody().get("content").get(0).get("matchPercent").asInt();
		int secondMatchPercent = response.getBody().get("content").get(1).get("matchPercent").asInt();
		assertThat(firstMatchPercent).isGreaterThanOrEqualTo(secondMatchPercent);
	}

	@Test
	void shouldReturnMatchesByAssignmentId() {
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 55, 0.55));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 65, 0.65));

		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/assignment/" + savedAssignment.getId(), JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldDeleteMatch_whenItExists() {
		ResumeMatch saved = resumeMatchRepository
				.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 88, 0.88));

		ResponseEntity<Void> response = restTemplate.exchange("/api/resume-matches/" + saved.getId(), HttpMethod.DELETE,
				null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(resumeMatchRepository.existsById(saved.getId())).isFalse();
	}

	@Test
	void shouldReturn404_whenDeletingNonExistentMatch() {
		long nonExistentId = 9999L;

		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/resume-matches/" + nonExistentId,
				HttpMethod.DELETE, null, JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnEmptyPage_whenNoMatchesExistForResume() {
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/resume/" + savedResume.getId(), JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnEmptyPage_whenNoMatchesExistForAssignment() {
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/assignment/" + savedAssignment.getId(), JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches/statistics
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldReturnStatistics_whenNoMatchesExist() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/statistics", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("todayCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("lastWeekCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("lastMonthCount").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnStatistics_withCountsReflectingSeededData() {
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6));

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/statistics", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("todayCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("lastWeekCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("lastMonthCount").asLong()).isEqualTo(2);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches/topmatched
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void topMatched_shouldReturnEmptyList_whenNoMatchesExist() {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void topMatched_shouldExcludeMatchesWithDecisionNo() {
		ResumeMatch yesMatch = buildMatch(savedResume.getId(), savedAssignment.getId(), 85, 0.85);
		yesMatch.setDecision("yes");
		yesMatch.setJudgedAt(Instant.now());
		resumeMatchRepository.save(yesMatch);

		ResumeMatch noMatch = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 30, 0.3);
		noMatch.setDecision("no");
		noMatch.setJudgedAt(Instant.now());
		resumeMatchRepository.save(noMatch);

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(1);
		assertThat(response.getBody().get(0).get("matchPercent").asInt()).isEqualTo(85);
	}

	@Test
	void topMatched_shouldReturnExpectedFields() {
		ResumeMatch match = buildMatch(savedResume.getId(), savedAssignment.getId(), 90, 0.9);
		match.setDecision("strong_yes");
		match.setJudgedAt(Instant.now());
		resumeMatchRepository.save(match);

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode item = response.getBody().get(0);
		assertThat(item.get("resumeFileName").asText()).isEqualTo("match-test.pdf");
		assertThat(item.get("matchPercent").asInt()).isEqualTo(90);
		assertThat(item.get("judgedAt").isNull()).isFalse();
		assertThat(item.get("assignmentSeeker")).isNotNull();
		assertThat(item.get("assignmentSeeker").get("firstName").asText()).isEqualTo("Test");
	}

	@Test
	void topMatched_shouldExcludeMatchesWithNullDecision() {
		ResumeMatch nullDecision = buildMatch(savedResume.getId(), savedAssignment.getId(), 75, 0.75);
		resumeMatchRepository.save(nullDecision);

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void topMatched_shouldIncludeAllPositiveDecisionTypes() {
		long assignmentOffset = 0;
		for (String decision : new String[]{"maybe", "yes", "strong_yes"}) {
			ResumeMatch m = buildMatch(savedResume.getId(), savedAssignment.getId() + assignmentOffset++, 70, 0.7);
			m.setDecision(decision);
			m.setJudgedAt(Instant.now());
			resumeMatchRepository.save(m);
		}

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(3);
	}

	@Test
	void topMatched_shouldReturnAtMostFive() {
		for (int i = 0; i < 7; i++) {
			ResumeMatch m = buildMatch(savedResume.getId(), savedAssignment.getId() + i, 60 + i, 0.6 + i * 0.01);
			m.setDecision("yes");
			m.setJudgedAt(Instant.now().minusSeconds(i));
			resumeMatchRepository.save(m);
		}

		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(5);
	}
}
