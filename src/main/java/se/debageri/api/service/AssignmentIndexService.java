package se.debageri.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.AssignmentIndex;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.AssignmentIndexRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentIndexService {

	private final AssignmentIndexRepository assignmentIndexRepository;

	public List<AssignmentIndex> findAll() {
		return assignmentIndexRepository.findAll();
	}

	public AssignmentIndex findById(Long assignmentId) {
		return assignmentIndexRepository.findById(assignmentId).orElseThrow(
				() -> new ResourceNotFoundException("AssignmentIndex not found for assignmentId: " + assignmentId));
	}

	@Transactional
	public AssignmentIndex save(AssignmentIndex assignmentIndex) {
		return assignmentIndexRepository.save(assignmentIndex);
	}

	@Transactional
	public void delete(Long assignmentId) {
		if (!assignmentIndexRepository.existsById(assignmentId)) {
			throw new ResourceNotFoundException("AssignmentIndex not found for assignmentId: " + assignmentId);
		}
		assignmentIndexRepository.deleteById(assignmentId);
	}
}
