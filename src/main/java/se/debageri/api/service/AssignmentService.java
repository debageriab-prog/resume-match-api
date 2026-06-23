package se.debageri.api.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import se.debageri.api.dto.AssignmentTopMatchedDto;
import se.debageri.api.dto.StatisticsResponse;
import se.debageri.api.entity.Assignment;
import se.debageri.api.exception.AssignmentNotFoundException;
import se.debageri.api.exception.DuplicateResourceException;
import se.debageri.api.rabbit.AssignmentEventPublisher;
import se.debageri.api.repository.AssignmentIndexRepository;
import se.debageri.api.repository.AssignmentRepository;
import se.debageri.api.repository.AssignmentSpecification;
import se.debageri.api.repository.ResumeMatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

	private final AssignmentRepository assignmentRepository;
	private final ResumeMatchRepository resumeMatchRepository;
	private final AssignmentIndexRepository assignmentIndexRepository;
	private final ElasticJobSearchService elasticJobSearchService;
	private final AssignmentEventPublisher assignmentEventPublisher;

	public StatisticsResponse getStatistics() {
		LocalDate today = LocalDate.now(ZoneId.of("CET"));
		LocalDate startOfLastWeek = today.minusDays(7);
		LocalDate startOfLastMonth = today.minusDays(30);
		return new StatisticsResponse(assignmentRepository.count(),
				assignmentRepository.countByPublishedOnBetween(today, today),
				assignmentRepository.countByPublishedOnBetween(startOfLastWeek, today),
				assignmentRepository.countByPublishedOnBetween(startOfLastMonth, today));
	}

	public Page<Assignment> findAll(Long jobId, String title, String client, String location, String portal,
			Pageable pageable) {
		Specification<Assignment> spec = Specification.where(AssignmentSpecification.hasJobId(jobId))
				.and(AssignmentSpecification.titleContains(title)).and(AssignmentSpecification.clientContains(client))
				.and(AssignmentSpecification.locationContains(location)).and(AssignmentSpecification.hasPortal(portal));
		return assignmentRepository.findAll(spec, pageable);
	}

	public Assignment findById(Long id) {
		return assignmentRepository.findById(id).orElseThrow(() -> new AssignmentNotFoundException(id));
	}

	public List<AssignmentTopMatchedDto> getTopMatched() {
		Map<Long, Long> matchCounts = resumeMatchRepository.findAssignmentMatchCounts().stream()
				.collect(Collectors.toMap(ResumeMatchRepository.AssignmentMatchCountRow::getAssignmentId,
						ResumeMatchRepository.AssignmentMatchCountRow::getMatchCount));

		if (matchCounts.isEmpty()) {
			return List.of();
		}

		return assignmentRepository.findAllById(matchCounts.keySet()).stream()
				.sorted(Comparator.comparing(Assignment::getPublishedOn,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(5).map(a -> new AssignmentTopMatchedDto(a.getId(), a.getTitle(), a.getClient(),
						a.getPublishedOn(), matchCounts.get(a.getId())))
				.collect(Collectors.toList());
	}

	@Transactional
	public Assignment save(Assignment assignment) {
		if (assignment.getId() == null && assignmentRepository.existsByJobId(assignment.getJobId())) {
			throw new DuplicateResourceException("Assignment already exists with jobId: " + assignment.getJobId());
		}
		Assignment saved = assignmentRepository.save(assignment);
		long savedId = saved.getId();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				assignmentEventPublisher.publishAssignmentUpserted(savedId);
			}
		});
		return saved;
	}

	@Transactional
	public Assignment update(Long id, Assignment updated) {
		Assignment existing = findById(id);
		existing.setJobId(updated.getJobId());
		existing.setTitle(updated.getTitle());
		existing.setClient(updated.getClient());
		existing.setPublishedOn(updated.getPublishedOn());
		existing.setApplicationDeadline(updated.getApplicationDeadline());
		existing.setRole(updated.getRole());
		existing.setSeniorityLevel(updated.getSeniorityLevel());
		existing.setLocation(updated.getLocation());
		existing.setRemote(updated.getRemote());
		existing.setPeriodStart(updated.getPeriodStart());
		existing.setPeriodEnd(updated.getPeriodEnd());
		existing.setDescription(updated.getDescription());
		existing.setRequiredSkills(updated.getRequiredSkills());
		existing.setPreferredSkills(updated.getPreferredSkills());
		existing.setLanguages(updated.getLanguages());
		existing.setUrl(updated.getUrl());
		existing.setPortal(updated.getPortal());
		return assignmentRepository.save(existing);
	}

	@Transactional
	public void delete(long id) {
		if (!assignmentRepository.existsById(id)) {
			throw new AssignmentNotFoundException(id);
		}

		List<Long> ids = List.of(id);

		log.debug("Deleting ResumeMatch rows for assignmentId={}", id);
		resumeMatchRepository.deleteByAssignmentIdIn(ids);

		log.debug("Deleting Assignment row id={}", id);
		assignmentRepository.deleteByIdIn(ids);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					elasticJobSearchService.deleteByAssignmentIds(ids);
					log.info("Deleted assignment id={} from Elasticsearch", id);
					assignmentIndexRepository.deleteById(id);
					log.info("Deleted assignment index for id={}", id);
				} catch (Exception e) {
					log.error("Failed to delete assignment id={} from Elasticsearch: {}", id, e.getMessage(), e);
				}
			}
		});

		log.info("Deleted assignment id={}", id);
	}
}
