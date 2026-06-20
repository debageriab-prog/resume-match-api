package se.debageri.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.ResumeMatch;

@Repository
public interface ResumeMatchRepository extends JpaRepository<ResumeMatch, Long> {

	List<ResumeMatch> findByResumeId(Long resumeId);

	List<ResumeMatch> findByAssignmentId(Long assignmentId);

	Optional<ResumeMatch> findByResumeIdAndAssignmentId(Long resumeId, Long assignmentId);

	List<ResumeMatch> findByResumeIdOrderByMatchPercentDesc(Long resumeId);

	void deleteByResumeId(Long resumeId);

	void deleteByAssignmentIdIn(List<Long> assignmentIds);
}
