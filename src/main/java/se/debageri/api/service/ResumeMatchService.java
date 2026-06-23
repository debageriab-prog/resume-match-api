package se.debageri.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.debageri.api.dto.ResumeMatchTopMatchedDto;
import se.debageri.api.dto.StatisticsResponse;
import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.Resume;
import se.debageri.api.entity.ResumeMatch;
import se.debageri.api.exception.ResourceNotFoundException;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeMatchService {

	private final ResumeMatchRepository resumeMatchRepository;
	private final ResumeRepository resumeRepository;

	public StatisticsResponse getStatistics() {
		ZoneId cet = ZoneId.of("CET");
		Instant now = Instant.now();
		LocalDate today = LocalDate.now(cet);
		Instant startOfToday = today.atStartOfDay(cet).toInstant();
		Instant startOfLastWeek = today.minusDays(7).atStartOfDay(cet).toInstant();
		Instant startOfLastMonth = today.minusDays(30).atStartOfDay(cet).toInstant();
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

	public List<ResumeMatchTopMatchedDto> getTopMatched() {
		List<ResumeMatch> matches = resumeMatchRepository
				.findValidDecisionMatchesOrderByJudgedAtDesc(PageRequest.of(0, 5));

		if (matches.isEmpty()) {
			return List.of();
		}

		Set<Long> resumeIds = matches.stream().map(ResumeMatch::getResumeId).collect(Collectors.toSet());

		Map<Long, Resume> resumeMap = resumeRepository.findAllById(resumeIds).stream()
				.collect(Collectors.toMap(Resume::getId, r -> r));

		return matches.stream().map(rm -> {
			Resume resume = resumeMap.get(rm.getResumeId());
			String fileName = resume != null ? resume.getFileName() : null;
			AssignmentSeeker seeker = resume != null ? resume.getOwner() : null;
			return new ResumeMatchTopMatchedDto(seeker, fileName, rm.getMatchPercent(), rm.getJudgedAt());
		}).collect(Collectors.toList());
	}
}
