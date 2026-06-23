package se.debageri.api.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

	interface AssignmentMatchCountRow {
		Long getAssignmentId();
		Long getMatchCount();
	}

	interface ResumeMatchCountRow {
		Long getResumeId();
		Long getMatchCount();
	}

	@Query("SELECT rm.assignmentId as assignmentId, COUNT(rm.id) as matchCount " +
			"FROM ResumeMatch rm " +
			"WHERE rm.decision IS NOT NULL AND rm.decision <> 'no' " +
			"GROUP BY rm.assignmentId")
	List<AssignmentMatchCountRow> findAssignmentMatchCounts();

	@Query("SELECT rm.resumeId as resumeId, COUNT(rm.id) as matchCount " +
			"FROM ResumeMatch rm " +
			"WHERE rm.decision IS NOT NULL AND rm.decision <> 'no' " +
			"GROUP BY rm.resumeId")
	List<ResumeMatchCountRow> findResumeMatchCounts();

	@Query("SELECT rm FROM ResumeMatch rm WHERE rm.decision IS NOT NULL AND rm.decision <> 'no' ORDER BY rm.judgedAt DESC")
	List<ResumeMatch> findValidDecisionMatchesOrderByJudgedAtDesc(Pageable pageable);
}
