package com.resumematch.api.repository;

import com.resumematch.api.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {

    List<Application> findByStatusOrderByPositionAsc(String status);

    List<Application> findByJobId(String jobId);

    List<Application> findByResumeId(String resumeId);

    Optional<Application> findByJobIdAndResumeId(String jobId, String resumeId);

    boolean existsByJobIdAndResumeId(String jobId, String resumeId);
}
