package se.debageri.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.Assignment;
import se.debageri.api.exception.DuplicateResourceException;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.AssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

	private final AssignmentRepository assignmentRepository;

	public List<Assignment> findAll() {
		return assignmentRepository.findAll();
	}

	public Assignment findById(Long id) {
		return assignmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
	}

	public Assignment findByJobId(Long jobId) {
		return assignmentRepository.findByJobId(jobId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found with jobId: " + jobId));
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
	public void delete(Long id) {
		if (!assignmentRepository.existsById(id)) {
			throw new ResourceNotFoundException("Assignment", id);
		}
		assignmentRepository.deleteById(id);
	}
}
