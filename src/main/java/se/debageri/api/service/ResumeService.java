package se.debageri.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.Resume;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

	private final ResumeRepository resumeRepository;
	private final AssignmentSeekerService assignmentSeekerService;

	public List<Resume> findAll() {
		return resumeRepository.findAll();
	}

	public Resume findById(Long id) {
		return resumeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resume", id));
	}

	public List<Resume> findByOwnerId(Long ownerId) {
		assignmentSeekerService.findById(ownerId);
		return resumeRepository.findByOwnerId(ownerId);
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
	public void delete(Long id) {
		if (!resumeRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resume", id);
		}
		resumeRepository.deleteById(id);
	}
}
