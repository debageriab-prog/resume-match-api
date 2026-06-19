package se.debageri.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.entity.ResumeMatch;
import se.debageri.api.exception.DuplicateResourceException;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.ResumeMatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeMatchService {

	private final ResumeMatchRepository resumeMatchRepository;

	public List<ResumeMatch> findAll() {
		return resumeMatchRepository.findAll();
	}

	public ResumeMatch findById(Long id) {
		return resumeMatchRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ResumeMatch", id));
	}

	public List<ResumeMatch> findByResumeId(Long resumeId) {
		return resumeMatchRepository.findByResumeIdOrderByMatchPercentDesc(resumeId);
	}

	public List<ResumeMatch> findByAssignmentId(Long assignmentId) {
		return resumeMatchRepository.findByAssignmentId(assignmentId);
	}

	@Transactional
	public ResumeMatch save(ResumeMatch resumeMatch) {
		resumeMatchRepository.findByResumeIdAndAssignmentId(resumeMatch.getResumeId(), resumeMatch.getAssignmentId())
				.ifPresent(m -> {
					throw new DuplicateResourceException("ResumeMatch already exists for resumeId "
							+ resumeMatch.getResumeId() + " and assignmentId " + resumeMatch.getAssignmentId());
				});
		return resumeMatchRepository.save(resumeMatch);
	}

	@Transactional
	public ResumeMatch update(Long id, ResumeMatch updated) {
		ResumeMatch existing = findById(id);
		existing.setScore(updated.getScore());
		existing.setMatchPercent(updated.getMatchPercent());
		existing.setReasonsJson(updated.getReasonsJson());
		existing.setMissingMustHavesJson(updated.getMissingMustHavesJson());
		existing.setDecision(updated.getDecision());
		existing.setJudgedAt(updated.getJudgedAt());
		existing.setJudgeModel(updated.getJudgeModel());
		return resumeMatchRepository.save(existing);
	}

	@Transactional
	public void delete(Long id) {
		if (!resumeMatchRepository.existsById(id)) {
			throw new ResourceNotFoundException("ResumeMatch", id);
		}
		resumeMatchRepository.deleteById(id);
	}
}
