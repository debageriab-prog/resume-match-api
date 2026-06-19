package se.debageri.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.exception.DuplicateResourceException;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.AssignmentSeekerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentSeekerService {

	private final AssignmentSeekerRepository assignmentSeekerRepository;

	public List<AssignmentSeeker> findAll() {
		return assignmentSeekerRepository.findAll();
	}

	public AssignmentSeeker findById(Long id) {
		return assignmentSeekerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("AssignmentSeeker", id));
	}

	@Transactional
	public AssignmentSeeker save(AssignmentSeeker seeker) {
		if (seeker.getId() == null && assignmentSeekerRepository.existsByEmail(seeker.getEmail())) {
			throw new DuplicateResourceException("AssignmentSeeker already exists with email: " + seeker.getEmail());
		}
		return assignmentSeekerRepository.save(seeker);
	}

	@Transactional
	public AssignmentSeeker update(Long id, AssignmentSeeker updated) {
		AssignmentSeeker existing = findById(id);
		if (!existing.getEmail().equals(updated.getEmail())
				&& assignmentSeekerRepository.existsByEmail(updated.getEmail())) {
			throw new DuplicateResourceException("Email already in use: " + updated.getEmail());
		}
		existing.setFirstName(updated.getFirstName());
		existing.setLastName(updated.getLastName());
		existing.setEmail(updated.getEmail());
		return assignmentSeekerRepository.save(existing);
	}

	@Transactional
	public void delete(Long id) {
		if (!assignmentSeekerRepository.existsById(id)) {
			throw new ResourceNotFoundException("AssignmentSeeker", id);
		}
		assignmentSeekerRepository.deleteById(id);
	}
}
