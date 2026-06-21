package se.debageri.api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.Resume;
import se.debageri.api.exception.DuplicateResourceException;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.AssignmentSeekerRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentSeekerService {

	private final AssignmentSeekerRepository assignmentSeekerRepository;
	private final ResumeRepository resumeRepository;
	private final ResumeMatchRepository resumeMatchRepository;

	public Page<AssignmentSeeker> findAll(Pageable pageable) {
		return assignmentSeekerRepository.findAll(pageable);
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

		List<Resume> resumes = resumeRepository.findByOwnerId(id);
		List<Long> resumeIds = resumes.stream().map(Resume::getId).toList();

		if (!resumeIds.isEmpty()) {
			log.debug("Deleting ResumeMatch rows for resumeIds={}", resumeIds);
			resumeMatchRepository.deleteByResumeIdIn(resumeIds);

			log.debug("Deleting {} resume(s) for seekerId={}", resumeIds.size(), id);
			resumeRepository.deleteAllById(resumeIds);
		}

		assignmentSeekerRepository.deleteById(id);
		log.info("Deleted assignment seeker id={} with {} resume(s)", id, resumeIds.size());
	}
}
