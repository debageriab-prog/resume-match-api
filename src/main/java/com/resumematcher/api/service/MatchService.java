package com.resumematcher.api.service;

import com.resumematcher.api.entity.JobPosting;
import com.resumematcher.api.entity.Match;
import com.resumematcher.api.entity.Resume;
import com.resumematcher.api.entity.enums.MatchStatus;
import com.resumematcher.api.exception.DuplicateResourceException;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final ResumeService resumeService;
    private final JobPostingService jobPostingService;

    public List<Match> findAll() {
        return matchRepository.findAll();
    }

    public Match findById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match", id));
    }

    public List<Match> findByResumeId(Long resumeId) {
        resumeService.findById(resumeId);
        return matchRepository.findByResumeIdOrderByScoreDesc(resumeId);
    }

    public List<Match> findByJobPostingId(Long jobPostingId) {
        jobPostingService.findById(jobPostingId);
        return matchRepository.findByJobPostingId(jobPostingId);
    }

    @Transactional
    public Match create(Long resumeId, Long jobPostingId, Match match) {
        Resume resume = resumeService.findById(resumeId);
        JobPosting jobPosting = jobPostingService.findById(jobPostingId);

        matchRepository.findByResumeIdAndJobPostingId(resumeId, jobPostingId).ifPresent(m -> {
            throw new DuplicateResourceException(
                    "Match already exists for resume id " + resumeId + " and job posting id " + jobPostingId);
        });

        match.setResume(resume);
        match.setJobPosting(jobPosting);
        return matchRepository.save(match);
    }

    @Transactional
    public Match updateScore(Long id, Double score, String analysisDetails) {
        Match existing = findById(id);
        existing.setScore(score);
        existing.setAnalysisDetails(analysisDetails);
        existing.setStatus(MatchStatus.ANALYZED);
        return matchRepository.save(existing);
    }

    @Transactional
    public Match updateStatus(Long id, MatchStatus status) {
        Match existing = findById(id);
        existing.setStatus(status);
        return matchRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!matchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Match", id);
        }
        matchRepository.deleteById(id);
    }
}
