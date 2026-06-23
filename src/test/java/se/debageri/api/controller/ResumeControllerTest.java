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

import se.debageri.api.dto.ResumeUpdateRequest;
import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.NotificationType;
import se.debageri.api.entity.Resume;
import se.debageri.api.entity.ResumeMatch;
import se.debageri.api.rabbit.AssignmentEventPublisher;
import se.debageri.api.repository.AssignmentSeekerRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;
import se.debageri.api.service.OpenAiService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ResumeControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private AssignmentSeekerRepository seekerRepository;

	@Autowired
	private ResumeMatchRepository resumeMatchRepository;

	@MockBean
	private OpenAIClient openAIClient;

	@MockBean
	private AssignmentEventPublisher assignmentEventPublisher;

	@MockBean
	private OpenAiService openAiService;

	@BeforeEach
	void setUp() {
		resumeMatchRepository.deleteAll();
		resumeRepository.deleteAll();
		seekerRepository.deleteAll();
	}

	private AssignmentSeeker createSeeker(String email) {
		AssignmentSeeker seeker = new AssignmentSeeker();
		seeker.setFirstName("Test");
		seeker.setLastName("User");
		seeker.setEmail(email);
		return seekerRepository.save(seeker);
	}

	private Resume createResume(AssignmentSeeker owner, NotificationType type) {
		Resume resume = new Resume();
		resume.setOwner(owner);
		resume.setFileName("test-resume.pdf");
		resume.setContentType("application/pdf");
		resume.setNotificationType(type);
		resume.setExtractedText("Sample resume text");
		resume.setPdfBytes(new byte[]{});
		return resumeRepository.save(resume);
	}

	@Test
	void shouldReturnEmptyPage_whenNoResumesExist() {
		// Given — empty database

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("content").isArray()).isTrue();
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(0);
	}

	@Test
	void shouldReturnAllResumes_whenTheyExist() {
		// Given
		AssignmentSeeker seeker1 = createSeeker("user1@example.com");
		AssignmentSeeker seeker2 = createSeeker("user2@example.com");
		createResume(seeker1, NotificationType.User);
		createResume(seeker2, NotificationType.User);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldReturnResumeSummary_whenItExists() {
		// Given
		AssignmentSeeker seeker = createSeeker("resume.owner@example.com");
		Resume saved = createResume(seeker, NotificationType.User);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/" + saved.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("id").asLong()).isEqualTo(saved.getId());
		assertThat(response.getBody().get("notificationType").asText()).isEqualTo("User");
	}

	@Test
	void shouldReturn404_whenResumeDoesNotExist() {
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/" + nonExistentId, JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody().get("message").asText()).contains("9999");
	}

	@Test
	void shouldReturnResumesByOwnerId_whenOwnerExists() {
		// Given
		AssignmentSeeker seeker = createSeeker("owner.check@example.com");
		createResume(seeker, NotificationType.User);
		createResume(seeker, NotificationType.User);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/owner/" + seeker.getId(),
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalElements").asLong()).isEqualTo(2);
	}

	@Test
	void shouldReturn404_whenOwnerDoesNotExist() {
		// Given
		long nonExistentOwnerId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/owner/" + nonExistentOwnerId,
				JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldUpdateResume_whenValidDataIsProvided() {
		// Given
		AssignmentSeeker seeker = createSeeker("update.test@example.com");
		Resume saved = createResume(seeker, NotificationType.User);
		ResumeUpdateRequest updateRequest = new ResumeUpdateRequest("manager@example.com", NotificationType.Both);

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/resumes/" + saved.getId(), HttpMethod.PUT,
				new HttpEntity<>(updateRequest), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("notificationType").asText()).isEqualTo("Both");
	}

	@Test
	void shouldReturn404_whenUpdatingNonExistentResume() {
		// Given
		ResumeUpdateRequest updateRequest = new ResumeUpdateRequest(null, NotificationType.User);

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/resumes/9999", HttpMethod.PUT,
				new HttpEntity<>(updateRequest), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldDeleteResume_whenItExists() {
		// Given
		AssignmentSeeker seeker = createSeeker("delete.test@example.com");
		Resume saved = createResume(seeker, NotificationType.User);

		// When
		ResponseEntity<Void> response = restTemplate.exchange("/api/resumes/" + saved.getId(), HttpMethod.DELETE, null,
				Void.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(resumeRepository.existsById(saved.getId())).isFalse();
	}

	@Test
	void shouldDeleteOwnerSeeker_whenLastResumeIsDeleted() {
		// Given
		AssignmentSeeker seeker = createSeeker("last.resume@example.com");
		Resume saved = createResume(seeker, NotificationType.User);
		long seekerId = seeker.getId();

		// When
		restTemplate.exchange("/api/resumes/" + saved.getId(), HttpMethod.DELETE, null, Void.class);

		// Then — seeker should also be deleted since they have no remaining resumes
		assertThat(seekerRepository.existsById(seekerId)).isFalse();
	}

	@Test
	void shouldReturn404_whenDeletingNonExistentResume() {
		// Given
		long nonExistentId = 9999L;

		// When
		ResponseEntity<JsonNode> response = restTemplate.exchange("/api/resumes/" + nonExistentId, HttpMethod.DELETE,
				null, JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteOwnerSeeker_whenOtherResumesRemain() {
		// Given
		AssignmentSeeker seeker = createSeeker("multi.resume@example.com");
		Resume first = createResume(seeker, NotificationType.User);
		createResume(seeker, NotificationType.User);
		long seekerId = seeker.getId();

		// When — delete only the first resume
		restTemplate.exchange("/api/resumes/" + first.getId(), HttpMethod.DELETE, null, Void.class);

		// Then — seeker should still exist because they have another resume
		assertThat(seekerRepository.existsById(seekerId)).isTrue();
		assertThat(resumeRepository.countByOwnerId(seekerId)).isEqualTo(1);
	}

	@Test
	void shouldReturnManagerEmail_whenSetOnResume() {
		// Given
		AssignmentSeeker seeker = createSeeker("manager.email@example.com");
		Resume resume = new Resume();
		resume.setOwner(seeker);
		resume.setFileName("cv.pdf");
		resume.setContentType("application/pdf");
		resume.setNotificationType(NotificationType.Manager);
		resume.setManagerEmail("boss@company.com");
		resume.setPdfBytes(new byte[]{});
		Resume saved = resumeRepository.save(resume);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/" + saved.getId(), JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("managerEmail").asText()).isEqualTo("boss@company.com");
		assertThat(response.getBody().get("notificationType").asText()).isEqualTo("Manager");
	}

	@Test
	void shouldReturnStatistics_whenNoResumesExist() {
		// Given — empty database

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/statistics", JsonNode.class);

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
		AssignmentSeeker seeker = createSeeker("stats.test@example.com");
		createResume(seeker, NotificationType.User);
		createResume(seeker, NotificationType.User);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/statistics", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("totalCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("todayCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("lastWeekCount").asLong()).isEqualTo(2);
		assertThat(response.getBody().get("lastMonthCount").asLong()).isEqualTo(2);
	}

	@Test
	void topMatched_shouldReturnEmptyList_whenNoMatchedResumesExist() {
		// Given — resumes exist but no positive matches

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void topMatched_shouldReturnResumeWithExpectedFields() {
		// Given
		AssignmentSeeker seeker = createSeeker("top.matched@example.com");
		seeker.setFirstName("Jane");
		seeker.setLastName("Doe");
		seeker = seekerRepository.save(seeker);

		Resume resume = new Resume();
		resume.setOwner(seeker);
		resume.setFileName("jane-doe-cv.pdf");
		resume.setContentType("application/pdf");
		resume.setNotificationType(NotificationType.User);
		resume.setPdfBytes(new byte[]{});
		resume = resumeRepository.save(resume);

		ResumeMatch match = new ResumeMatch();
		match.setResumeId(resume.getId());
		match.setAssignmentId(1001L);
		match.setMatchPercent(88);
		match.setScore(0.88);
		match.setDecision("yes");
		resumeMatchRepository.save(match);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(1);
		JsonNode item = response.getBody().get(0);
		assertThat(item.get("fileName").asText()).isEqualTo("jane-doe-cv.pdf");
		assertThat(item.get("ownerName").asText()).isEqualTo("Jane Doe");
		assertThat(item.get("createdAt").isNull()).isFalse();
		assertThat(item.get("matchCount").asLong()).isEqualTo(1);
	}

	@Test
	void topMatched_shouldExcludeResumesWithOnlyNoDecision() {
		// Given
		AssignmentSeeker seeker = createSeeker("no.decision@example.com");
		Resume resume = createResume(seeker, NotificationType.User);

		ResumeMatch noMatch = new ResumeMatch();
		noMatch.setResumeId(resume.getId());
		noMatch.setAssignmentId(2001L);
		noMatch.setMatchPercent(25);
		noMatch.setScore(0.25);
		noMatch.setDecision("no");
		resumeMatchRepository.save(noMatch);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(0);
	}

	@Test
	void topMatched_shouldCountOnlyPositiveDecisions() {
		// Given
		AssignmentSeeker seeker = createSeeker("mixed.decisions@example.com");
		Resume resume = createResume(seeker, NotificationType.User);

		// Two positive matches
		for (String decision : new String[]{"yes", "maybe"}) {
			ResumeMatch m = new ResumeMatch();
			m.setResumeId(resume.getId());
			m.setAssignmentId((long) decision.hashCode() + 5000);
			m.setMatchPercent(70);
			m.setScore(0.7);
			m.setDecision(decision);
			resumeMatchRepository.save(m);
		}
		// One "no" that should not be counted
		ResumeMatch noMatch = new ResumeMatch();
		noMatch.setResumeId(resume.getId());
		noMatch.setAssignmentId(9999L);
		noMatch.setMatchPercent(20);
		noMatch.setScore(0.2);
		noMatch.setDecision("no");
		resumeMatchRepository.save(noMatch);

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().size()).isEqualTo(1);
		assertThat(response.getBody().get(0).get("matchCount").asLong()).isEqualTo(2);
	}

	@Test
	void topMatched_shouldReturnAtMostFive() {
		// Given — 7 resumes each with a positive match
		for (int i = 0; i < 7; i++) {
			AssignmentSeeker s = createSeeker("seeker" + i + "@test.com");
			Resume r = createResume(s, NotificationType.User);

			ResumeMatch m = new ResumeMatch();
			m.setResumeId(r.getId());
			m.setAssignmentId((long) (10000 + i));
			m.setMatchPercent(60 + i);
			m.setScore(0.6 + i * 0.01);
			m.setDecision("yes");
			resumeMatchRepository.save(m);
		}

		// When
		ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/resumes/topmatched", JsonNode.class);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().isArray()).isTrue();
		assertThat(response.getBody().size()).isEqualTo(5);
	}
}
