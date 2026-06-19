package com.resumematcher.api.service;

import com.resumematcher.api.entity.Candidate;
import com.resumematcher.api.entity.Experience;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.ExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final CandidateService candidateService;

    public List<Experience> findAll() {
        return experienceRepository.findAll();
    }

    public Experience findById(Long id) {
        return experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id));
    }

    public List<Experience> findByCandidateId(Long candidateId) {
        candidateService.findById(candidateId);
        return experienceRepository.findByCandidateIdOrderByStartDateDesc(candidateId);
    }

    @Transactional
    public Experience create(Long candidateId, Experience experience) {
        Candidate candidate = candidateService.findById(candidateId);
        experience.setCandidate(candidate);
        return experienceRepository.save(experience);
    }

    @Transactional
    public Experience update(Long id, Experience updatedExperience) {
        Experience existing = findById(id);
        existing.setCompanyName(updatedExperience.getCompanyName());
        existing.setJobTitle(updatedExperience.getJobTitle());
        existing.setDescription(updatedExperience.getDescription());
        existing.setLocation(updatedExperience.getLocation());
        existing.setStartDate(updatedExperience.getStartDate());
        existing.setEndDate(updatedExperience.getEndDate());
        existing.setIsCurrent(updatedExperience.getIsCurrent());
        return experienceRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!experienceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Experience", id);
        }
        experienceRepository.deleteById(id);
    }
}
