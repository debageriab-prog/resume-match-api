package se.debageri.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.dto.ResumeMatchDto;
import se.debageri.api.dto.ResumeMatchTopMatchedDto;
import se.debageri.api.dto.StatisticsResponse;
import se.debageri.api.entity.Assignment;
import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.Resume;
import se.debageri.api.entity.ResumeMatch;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.AssignmentRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeMatchService {

	private static final Set<String> VALID_DECISIONS = Set.of("no", "maybe", "yes", "strong_yes");

	private final ResumeMatchRepository resumeMatchRepository;
	private final ResumeRepository resumeRepository;
	private final AssignmentRepository assignmentRepository;

	public StatisticsResponse getStatistics() {
		ZoneId cet = ZoneId.of("CET");
		Instant now = Instant.now();
		LocalDate today = LocalDate.now(cet);
		Instant startOfToday = today.atStartOfDay(cet).toInstant();
		Instant startOfLastWeek = today.minusDays(7).atStartOfDay(cet).toInstant();
		Instant startOfLastMonth = today.minusDays(30).atStartOfDay(cet).toInstant();
		return new StatisticsResponse(resumeMatchRepository.count(),
				resumeMatchRepository.countByMatchedAtBetween(startOfToday, now),
				resumeMatchRepository.countByMatchedAtBetween(startOfLastWeek, now),
				resumeMatchRepository.countByMatchedAtBetween(startOfLastMonth, now));
	}

	public Page<ResumeMatchDto> findAll(Long assignmentId, Long resumeId, String assignmentTitle, String resumeFileName,
			String ownerName, Boolean includeNegativeDecisions, String decision, Pageable pageable) {
		if (decision != null && !VALID_DECISIONS.contains(decision)) {
			throw new IllegalArgumentException(
					"Invalid decision value: '" + decision + "'. Allowed values: no, maybe, yes, strong_yes");
		}
		Specification<ResumeMatch> spec = buildSpec(assignmentId, resumeId, assignmentTitle, resumeFileName, ownerName,
				includeNegativeDecisions, decision);
		Page<ResumeMatch> page = resumeMatchRepository.findAll(spec, pageable);
		return enrichPage(page);
	}

	public ResumeMatchDto findById(Long id) {
		ResumeMatch rm = resumeMatchRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ResumeMatch", id));
		return enrichSingle(rm);
	}

	public Page<ResumeMatchDto> findByResumeId(Long resumeId, Pageable pageable) {
		Page<ResumeMatch> page = resumeMatchRepository.findByResumeId(resumeId, pageable);
		return enrichPage(page);
	}

	public Page<ResumeMatchDto> findByAssignmentId(Long assignmentId, Pageable pageable) {
		Page<ResumeMatch> page = resumeMatchRepository.findByAssignmentId(assignmentId, pageable);
		return enrichPage(page);
	}

	@Transactional
	public void delete(Long id) {
		if (!resumeMatchRepository.existsById(id)) {
			throw new ResourceNotFoundException("ResumeMatch", id);
		}
		resumeMatchRepository.deleteById(id);
	}

	public List<ResumeMatchTopMatchedDto> getTopMatched() {
		List<ResumeMatch> matches = resumeMatchRepository
				.findValidDecisionMatchesOrderByJudgedAtDesc(PageRequest.of(0, 5));

		if (matches.isEmpty()) {
			return List.of();
		}

		Set<Long> resumeIds = matches.stream().map(ResumeMatch::getResumeId).collect(Collectors.toSet());

		Map<Long, Resume> resumeMap = resumeRepository.findAllById(resumeIds).stream()
				.collect(Collectors.toMap(Resume::getId, r -> r));

		return matches.stream().map(rm -> {
			Resume resume = resumeMap.get(rm.getResumeId());
			String fileName = resume != null ? resume.getFileName() : null;
			AssignmentSeeker seeker = resume != null ? resume.getOwner() : null;
			return new ResumeMatchTopMatchedDto(seeker, fileName, rm.getMatchPercent(), rm.getJudgedAt());
		}).collect(Collectors.toList());
	}

	private Page<ResumeMatchDto> enrichPage(Page<ResumeMatch> page) {
		if (page.isEmpty()) {
			return page.map(rm -> toDto(rm, Map.of(), Map.of()));
		}

		Set<Long> resumeIds = page.stream().map(ResumeMatch::getResumeId).collect(Collectors.toSet());
		Set<Long> assignmentIds = page.stream().map(ResumeMatch::getAssignmentId).collect(Collectors.toSet());

		Map<Long, Resume> resumeMap = resumeRepository.findAllById(resumeIds).stream()
				.collect(Collectors.toMap(Resume::getId, r -> r));
		Map<Long, Assignment> assignmentMap = assignmentRepository.findAllById(assignmentIds).stream()
				.collect(Collectors.toMap(Assignment::getId, a -> a));

		return page.map(rm -> toDto(rm, resumeMap, assignmentMap));
	}

	private ResumeMatchDto enrichSingle(ResumeMatch rm) {
		Resume resume = resumeRepository.findById(rm.getResumeId()).orElse(null);
		Assignment assignment = assignmentRepository.findById(rm.getAssignmentId()).orElse(null);
		Map<Long, Resume> resumeMap = resume != null ? Map.of(resume.getId(), resume) : Map.of();
		Map<Long, Assignment> assignmentMap = assignment != null ? Map.of(assignment.getId(), assignment) : Map.of();
		return toDto(rm, resumeMap, assignmentMap);
	}

	private ResumeMatchDto toDto(ResumeMatch rm, Map<Long, Resume> resumeMap, Map<Long, Assignment> assignmentMap) {
		Assignment assignment = assignmentMap.get(rm.getAssignmentId());
		Resume resume = resumeMap.get(rm.getResumeId());

		ResumeMatchDto.AssignmentSummary assignmentSummary = new ResumeMatchDto.AssignmentSummary(
				assignment != null ? assignment.getId() : rm.getAssignmentId(),
				assignment != null ? assignment.getTitle() : null);

		String ownerFullName = null;
		if (resume != null && resume.getOwner() != null) {
			ownerFullName = resume.getOwner().getFirstName() + " " + resume.getOwner().getLastName();
		}
		ResumeMatchDto.ResumeSummary resumeSummary = new ResumeMatchDto.ResumeSummary(
				resume != null ? resume.getId() : rm.getResumeId(), resume != null ? resume.getFileName() : null,
				ownerFullName);

		return new ResumeMatchDto(rm.getId(), assignmentSummary, resumeSummary, rm.getScore(), rm.getMatchPercent(),
				rm.getMatchedAt(), rm.getReasonsJson(), rm.getMissingMustHavesJson(), rm.getDecision(),
				rm.getJudgedAt(), rm.getJudgeModel());
	}

	private static Specification<ResumeMatch> buildSpec(Long assignmentId, Long resumeId, String assignmentTitle,
			String resumeFileName, String ownerName, Boolean includeNegativeDecisions, String decision) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (assignmentId != null) {
				predicates.add(cb.equal(root.get("assignmentId"), assignmentId));
			}
			if (resumeId != null) {
				predicates.add(cb.equal(root.get("resumeId"), resumeId));
			}

			if (assignmentTitle != null && !assignmentTitle.isBlank()) {
				Subquery<Long> sub = query.subquery(Long.class);
				Root<Assignment> aRoot = sub.from(Assignment.class);
				sub.select(aRoot.<Long>get("id"))
						.where(cb.like(cb.lower(aRoot.get("title")), "%" + assignmentTitle.toLowerCase() + "%"));
				predicates.add(root.get("assignmentId").in(sub));
			}

			if (resumeFileName != null && !resumeFileName.isBlank()) {
				Subquery<Long> sub = query.subquery(Long.class);
				Root<Resume> rRoot = sub.from(Resume.class);
				sub.select(rRoot.<Long>get("id"))
						.where(cb.like(cb.lower(rRoot.get("fileName")), "%" + resumeFileName.toLowerCase() + "%"));
				predicates.add(root.get("resumeId").in(sub));
			}

			if (ownerName != null && !ownerName.isBlank()) {
				Subquery<Long> sub = query.subquery(Long.class);
				Root<Resume> rRoot = sub.from(Resume.class);
				Join<Resume, AssignmentSeeker> ownerJoin = rRoot.join("owner");
				predicates.add(root.get("resumeId")
						.in(sub.select(rRoot.<Long>get("id"))
								.where(cb.like(
										cb.lower(cb.concat(cb.concat(ownerJoin.<String>get("firstName"), " "),
												ownerJoin.<String>get("lastName"))),
										"%" + ownerName.toLowerCase() + "%"))));
			}

			// Default: exclude null decisions and "no" decisions.
			// Pass includeNegativeDecisions=true to include them.
			if (!Boolean.TRUE.equals(includeNegativeDecisions)) {
				predicates.add(cb.isNotNull(root.get("decision")));
				predicates.add(cb.notEqual(root.get("decision"), "no"));
			}

			if (decision != null) {
				predicates.add(cb.equal(root.get("decision"), decision));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
