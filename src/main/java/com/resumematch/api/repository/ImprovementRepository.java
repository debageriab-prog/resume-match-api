package com.resumematch.api.repository;

import com.resumematch.api.entity.Improvement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImprovementRepository extends JpaRepository<Improvement, String> {

    List<Improvement> findByOriginalResumeId(String originalResumeId);

    List<Improvement> findByTailoredResumeId(String tailoredResumeId);

    List<Improvement> findByJobId(String jobId);
}
