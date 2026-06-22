package se.debageri.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.dto.StatisticsResponse;
import se.debageri.api.entity.ResumeMatch;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.ResumeMatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeMatchService {

	private final ResumeMatchRepository resumeMatchRepository;

	public StatisticsResponse getStatistics() {
		Instant now = Instant.now();
		Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant startOfLastWeek = now.minusSeconds(7 * 24 * 60 * 60);
		Instant startOfLastMonth = now.minusSeconds(30L * 24 * 60 * 60);
		return new StatisticsResponse(resumeMatchRepository.count(),
				resumeMatchRepository.countByMatchedAtBetween(startOfToday, now),
				resumeMatchRepository.countByMatchedAtBetween(startOfLastWeek, now),
				resumeMatchRepository.countByMatchedAtBetween(startOfLastMonth, now));
	}

	public Page<ResumeMatch> findAll(Pageable pageable) {
		return resumeMatchRepository.findAll(pageable);
	}

	public ResumeMatch findById(Long id) {
		return resumeMatchRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ResumeMatch", id));
	}

	public Page<ResumeMatch> findByResumeId(Long resumeId, Pageable pageable) {
		return resumeMatchRepository.findByResumeId(resumeId, pageable);
	}

	public Page<ResumeMatch> findByAssignmentId(Long assignmentId, Pageable pageable) {
		return resumeMatchRepository.findByAssignmentId(assignmentId, pageable);
	}

	@Transactional
	public void delete(Long id) {
		if (!resumeMatchRepository.existsById(id)) {
			throw new ResourceNotFoundException("ResumeMatch", id);
		}
		resumeMatchRepository.deleteById(id);
	}
}
