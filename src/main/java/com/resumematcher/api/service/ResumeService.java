package com.resumematcher.api.service;

import com.resumematcher.api.entity.Candidate;
import com.resumematcher.api.entity.Resume;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final CandidateService candidateService;

    public List<Resume> findAll() {
        return resumeRepository.findAll();
    }

    public Resume findById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", id));
    }

    public List<Resume> findByCandidateId(Long candidateId) {
        candidateService.findById(candidateId);
        return resumeRepository.findByCandidateId(candidateId);
    }

    @Transactional
    public Resume create(Long candidateId, Resume resume) {
        Candidate candidate = candidateService.findById(candidateId);
        resume.setCandidate(candidate);
        return resumeRepository.save(resume);
    }

    @Transactional
    public Resume update(Long id, Resume updatedResume) {
        Resume existing = findById(id);
        existing.setTitle(updatedResume.getTitle());
        existing.setSummary(updatedResume.getSummary());
        existing.setFileUrl(updatedResume.getFileUrl());
        existing.setParsedContent(updatedResume.getParsedContent());
        existing.setIsActive(updatedResume.getIsActive());
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
