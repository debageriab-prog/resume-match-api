package se.debageri.api.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.ResumeMatch;

@Repository
public interface ResumeMatchRepository extends JpaRepository<ResumeMatch, Long> {

	Page<ResumeMatch> findByResumeId(Long resumeId, Pageable pageable);

	Page<ResumeMatch> findByAssignmentId(Long assignmentId, Pageable pageable);

	List<ResumeMatch> findByResumeIdOrderByMatchPercentDesc(Long resumeId);

	void deleteByResumeId(Long resumeId);

	void deleteByResumeIdIn(List<Long> resumeIds);

	void deleteByAssignmentIdIn(List<Long> assignmentIds);

	long countByMatchedAtBetween(Instant start, Instant end);
}
