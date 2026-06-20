package se.debageri.api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.Assignment;
import se.debageri.api.exception.AssignmentNotFoundException;
import se.debageri.api.exception.DuplicateResourceException;
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

	@Transactional
	public Assignment save(Assignment assignment) {
		if (assignment.getId() == null && assignmentRepository.existsByJobId(assignment.getJobId())) {
			throw new DuplicateResourceException("Assignment already exists with jobId: " + assignment.getJobId());
		}
		return assignmentRepository.save(assignment);
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

		log.info("Deleted assignment id={}", id);
	}
}
