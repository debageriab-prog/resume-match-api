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
		// Given — no matches in database

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("content").isArray()).isTrue();
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnAllMatches_whenTheyExist() {
		// Given
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 85, 0.85));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldReturnEnrichedAssignmentAndResume_inGetAll() {
		// Given
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JsonNode item = response.getBody().get("content").get(0);
		assertThat(item.get("assignment")).isNotNull();
		assertThat(item.get("assignment").get("id").asLong()).isEqualTo(savedAssignment.getId());
		assertThat(item.get("assignment").get("title").asText()).isEqualTo("Test Role");
		assertThat(item.get("resume")).isNotNull();
		assertThat(item.get("resume").get("id").asLong()).isEqualTo(savedResume.getId());
		assertThat(item.get("resume").get("fileName").asText()).isEqualTo("match-test.pdf");
		assertThat(item.get("resume").get("ownerFullName").asText()).isEqualTo("Test Seeker");
		assertThat(item.has("assignmentId")).isFalse();
		assertThat(item.has("resumeId")).isFalse();
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches — filtering
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldFilterByAssignmentId() {
		// Given
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6));

		// When
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches?assignmentId=" + savedAssignment.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("assignment").get("id").asLong())
				.isEqualTo(savedAssignment.getId());
	}

	@Test
	void shouldFilterByResumeId() {
		// Given
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 60, 0.6));

		// When
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches?resumeId=" + savedResume.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("resume").get("id").asLong())
				.isEqualTo(savedResume.getId());
	}

	@Test
	void shouldFilterByDecisionNotNull_true() {
		// Given
		ResumeMatch withDecision = buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8);
		withDecision.setDecision("yes");
		resumeMatchRepository.save(withDecision);

		ResumeMatch withoutDecision = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6);
		resumeMatchRepository.save(withoutDecision);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches?decisionNotNull=true",
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("decision").asText()).isEqualTo("yes");
	}

	@Test
	void shouldFilterByDecisionNotNull_false() {
		// Given
		ResumeMatch withDecision = buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8);
		withDecision.setDecision("yes");
		resumeMatchRepository.save(withDecision);

		ResumeMatch withoutDecision = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6);
		resumeMatchRepository.save(withoutDecision);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches?decisionNotNull=false",
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		JsonNode decisionNode = response.getBody().get("content").get(0).path("decision");
		assertThat(decisionNode.isNull() || decisionNode.isMissingNode()).isTrue();
	}

	@Test
	void shouldFilterByDecisionValue() {
		// Given
		for (String dec : new String[]{"no", "maybe", "yes", "strong_yes"}) {
			ResumeMatch m = buildMatch(savedResume.getId(), savedAssignment.getId() + dec.length(), 70, 0.7);
			m.setDecision(dec);
			resumeMatchRepository.save(m);
		}

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches?decision=strong_yes",
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(1);
		assertThat(response.getBody().get("content").get(0).get("decision").asText()).isEqualTo("strong_yes");
	}

	@Test
	void shouldReturn400_whenDecisionValueIsInvalid() {
		// Given — no setup needed

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches?decision=invalid_value",
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().get("message").asText()).contains("invalid_value");
	}

	@Test
	void shouldSupportCombinedFilters_assignmentIdAndDecision() {
		// Given
		ResumeMatch match1 = buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8);
		match1.setDecision("yes");
		resumeMatchRepository.save(match1);

		ResumeMatch match2 = buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 70, 0.7);
		match2.setDecision("no");
		resumeMatchRepository.save(match2);

		ResumeMatch match3 = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6);
		match3.setDecision("yes");
		resumeMatchRepository.save(match3);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity(
				"/api/resume-matches?assignmentId=" + savedAssignment.getId() + "&decision=yes", JsonNode.class);

		// Then
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
		// Given
		ResumeMatch saved = resumeMatchRepository
				.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 75, 0.75));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/" + saved.getId(),
				JsonNode.class);

		// Then
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
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/" + nonExistentId,
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().get("message").asText()).contains("9999");
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches/resume/{resumeId}
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldReturnMatchesByResumeId_sortedByMatchPercentDesc() {
		// Given
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 50, 0.5));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 90, 0.9));
		resumeMatchRepository.save(buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 70, 0.7));

		// When
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/resume/" + savedResume.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
		int firstMatchPercent = response.getBody().get("content").get(0).get("matchPercent").asInt();
		int secondMatchPercent = response.getBody().get("content").get(1).get("matchPercent").asInt();
		assertThat(firstMatchPercent).isGreaterThanOrEqualTo(secondMatchPercent);
	}

	@Test
	void shouldReturnMatchesByAssignmentId() {
		// Given
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId() + 1, savedAssignment.getId(), 55, 0.55));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 65, 0.65));

		// When
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/assignment/" + savedAssignment.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldDeleteMatch_whenItExists() {
		// Given
		ResumeMatch saved = resumeMatchRepository
				.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 88, 0.88));

		// When
		ResponseEntity<Void> response = restTemplate.exchange("/api/resume-matches/" + saved.getId(), HttpMethod.DELETE,
				null, Void.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(resumeMatchRepository.existsById(saved.getId())).isFalse();
	}

	@Test
	void shouldReturn404_whenDeletingNonExistentMatch() {
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/resume-matches/" + nonExistentId,
				HttpMethod.DELETE, null, JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldReturnEmptyPage_whenNoMatchesExistForResume() {
		// Given — resume exists but has no matches

		// When
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/resume/" + savedResume.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnEmptyPage_whenNoMatchesExistForAssignment() {
		// Given — assignment exists but has no matches

		// When
		ResponseEntity<JsonNode> response = restTemplate
				.getForEntity("/api/resume-matches/assignment/" + savedAssignment.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// GET /api/resume-matches/statistics
	// ──────────────────────────────────────────────────────────────────────────

	@Test
	void shouldReturnStatistics_whenNoMatchesExist() {
		// Given — empty matches (setUp already runs but creates no matches)

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/statistics", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("todayCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("lastWeekCount").asLong()).isEqualTo(0);
		assertThat(response.getBody().get("lastMonthCount").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnStatistics_withCountsReflectingSeededData() {
		// Given
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId(), 80, 0.8));
		resumeMatchRepository.save(buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 60, 0.6));

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/statistics", JsonNode.class);

		// Then
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
		// Given — no matches

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void topMatched_shouldExcludeMatchesWithDecisionNo() {
		// Given — one match with "yes", one with "no"
		ResumeMatch yesMatch = buildMatch(savedResume.getId(), savedAssignment.getId(), 85, 0.85);
		yesMatch.setDecision("yes");
		yesMatch.setJudgedAt(Instant.now());
		resumeMatchRepository.save(yesMatch);

		ResumeMatch noMatch = buildMatch(savedResume.getId(), savedAssignment.getId() + 1, 30, 0.3);
		noMatch.setDecision("no");
		noMatch.setJudgedAt(Instant.now());
		resumeMatchRepository.save(noMatch);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(1);
		assertThat(response.getBody().get(0).get("matchPercent").asInt()).isEqualTo(85);
	}

	@Test
	void topMatched_shouldReturnExpectedFields() {
		// Given
		ResumeMatch match = buildMatch(savedResume.getId(), savedAssignment.getId(), 90, 0.9);
		match.setDecision("strong_yes");
		match.setJudgedAt(Instant.now());
		resumeMatchRepository.save(match);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		// Then
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
		// Given — match with null decision
		ResumeMatch nullDecision = buildMatch(savedResume.getId(), savedAssignment.getId(), 75, 0.75);
		resumeMatchRepository.save(nullDecision);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void topMatched_shouldIncludeAllPositiveDecisionTypes() {
		// Given — three matches with maybe, yes, strong_yes
		long assignmentOffset = 0;
		for (String decision : new String[]{"maybe", "yes", "strong_yes"}) {
			ResumeMatch m = buildMatch(savedResume.getId(), savedAssignment.getId() + assignmentOffset++, 70, 0.7);
			m.setDecision(decision);
			m.setJudgedAt(Instant.now());
			resumeMatchRepository.save(m);
		}

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(3);
	}

	@Test
	void topMatched_shouldReturnAtMostFive() {
		// Given — 7 matches all with "yes"
		for (int i = 0; i < 7; i++) {
			ResumeMatch m = buildMatch(savedResume.getId(), savedAssignment.getId() + i, 60 + i, 0.6 + i * 0.01);
			m.setDecision("yes");
			m.setJudgedAt(Instant.now().minusSeconds(i));
			resumeMatchRepository.save(m);
		}

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resume-matches/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(5);
	}
}
