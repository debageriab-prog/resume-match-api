package se.debageri.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

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
}
