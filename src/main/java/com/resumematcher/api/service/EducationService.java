package com.resumematcher.api.service;

import com.resumematcher.api.entity.Candidate;
import com.resumematcher.api.entity.Education;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.EducationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EducationService {

    private final EducationRepository educationRepository;
    private final CandidateService candidateService;

    public List<Education> findAll() {
        return educationRepository.findAll();
    }

    public Education findById(Long id) {
        return educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education", id));
    }

    public List<Education> findByCandidateId(Long candidateId) {
        candidateService.findById(candidateId);
        return educationRepository.findByCandidateIdOrderByStartDateDesc(candidateId);
    }

    @Transactional
    public Education create(Long candidateId, Education education) {
        Candidate candidate = candidateService.findById(candidateId);
        education.setCandidate(candidate);
        return educationRepository.save(education);
    }

    @Transactional
    public Education update(Long id, Education updatedEducation) {
        Education existing = findById(id);
        existing.setInstitution(updatedEducation.getInstitution());
        existing.setDegree(updatedEducation.getDegree());
        existing.setFieldOfStudy(updatedEducation.getFieldOfStudy());
        existing.setDescription(updatedEducation.getDescription());
        existing.setStartDate(updatedEducation.getStartDate());
        existing.setEndDate(updatedEducation.getEndDate());
        existing.setIsCurrent(updatedEducation.getIsCurrent());
        return educationRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!educationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Education", id);
        }
        educationRepository.deleteById(id);
    }
}
