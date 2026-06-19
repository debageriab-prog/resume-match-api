package com.resumematcher.api.repository;

import com.resumematcher.api.entity.Match;
import com.resumematcher.api.entity.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByResumeId(Long resumeId);

    List<Match> findByJobPostingId(Long jobPostingId);

    List<Match> findByStatus(MatchStatus status);

    Optional<Match> findByResumeIdAndJobPostingId(Long resumeId, Long jobPostingId);

    List<Match> findByResumeIdOrderByScoreDesc(Long resumeId);
}
