package com.resumematcher.api.service;

import com.resumematcher.api.entity.Candidate;
import com.resumematcher.api.exception.DuplicateResourceException;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public List<Candidate> findAll() {
        return candidateRepository.findAll();
    }

    public Candidate findById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", id));
    }

    public Candidate findByEmail(String email) {
        return candidateRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with email: " + email));
    }

    @Transactional
    public Candidate create(Candidate candidate) {
        if (candidateRepository.existsByEmail(candidate.getEmail())) {
            throw new DuplicateResourceException("Candidate already exists with email: " + candidate.getEmail());
        }
        return candidateRepository.save(candidate);
    }

    @Transactional
    public Candidate update(Long id, Candidate updatedCandidate) {
        Candidate existing = findById(id);

        if (!existing.getEmail().equals(updatedCandidate.getEmail())
                && candidateRepository.existsByEmail(updatedCandidate.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + updatedCandidate.getEmail());
        }

        existing.setFirstName(updatedCandidate.getFirstName());
        existing.setLastName(updatedCandidate.getLastName());
        existing.setEmail(updatedCandidate.getEmail());
        existing.setPhone(updatedCandidate.getPhone());
        existing.setLinkedinUrl(updatedCandidate.getLinkedinUrl());
        existing.setPortfolioUrl(updatedCandidate.getPortfolioUrl());
        existing.setLocation(updatedCandidate.getLocation());
        existing.setSummary(updatedCandidate.getSummary());

        return candidateRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!candidateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Candidate", id);
        }
        candidateRepository.deleteById(id);
    }
}
