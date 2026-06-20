package se.debageri.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.Resume;
import se.debageri.api.exception.ResumeNotFoundException;
import se.debageri.api.repository.AssignmentSeekerRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

	private final ResumeRepository resumeRepository;
	private final ResumeMatchRepository resumeMatchRepository;
	private final AssignmentSeekerRepository assignmentSeekerRepository;
	private final AssignmentSeekerService assignmentSeekerService;

	public Page<Resume> findAll(Pageable pageable) {
		return resumeRepository.findAll(pageable);
	}

	public Resume findById(Long id) {
		return resumeRepository.findById(id).orElseThrow(() -> new ResumeNotFoundException(id));
	}

	public Page<Resume> findByOwnerId(Long ownerId, Pageable pageable) {
		assignmentSeekerService.findById(ownerId);
		return resumeRepository.findAll((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId), pageable);
	}

	@Transactional
	public Resume save(Long ownerId, Resume resume) {
		AssignmentSeeker owner = assignmentSeekerService.findById(ownerId);
		resume.setOwner(owner);
		return resumeRepository.save(resume);
	}

	@Transactional
	public Resume update(Long id, Resume updated) {
		Resume existing = findById(id);
		existing.setFileName(updated.getFileName());
		existing.setContentType(updated.getContentType());
		existing.setPdfBytes(updated.getPdfBytes());
		existing.setExtractedText(updated.getExtractedText());
		existing.setProfileJson(updated.getProfileJson());
		existing.setManagerEmail(updated.getManagerEmail());
		existing.setNotificationType(updated.getNotificationType());
		return resumeRepository.save(existing);
	}

	@Transactional
	public void delete(long resumeId) {
		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId));

		Long ownerId = resume.getOwner() != null ? resume.getOwner().getId() : null;

		resumeMatchRepository.deleteByResumeId(resumeId);

		resumeRepository.deleteById(resumeId);

		if (ownerId != null) {
			long remaining = resumeRepository.countByOwnerId(ownerId);
			if (remaining == 0) {
				assignmentSeekerRepository.deleteById(ownerId);
				log.info("Deleted owner assignmentSeeker id={}", ownerId);
			}
		}

		log.info("Deleted resume id={}", resumeId);
	}
}
