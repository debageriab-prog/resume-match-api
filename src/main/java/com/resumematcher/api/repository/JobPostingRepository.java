package com.resumematcher.api.repository;

import com.resumematcher.api.entity.JobPosting;
import com.resumematcher.api.entity.enums.ExperienceLevel;
import com.resumematcher.api.entity.enums.JobStatus;
import com.resumematcher.api.entity.enums.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    List<JobPosting> findByCompanyId(Long companyId);

    List<JobPosting> findByStatus(JobStatus status);

    List<JobPosting> findByJobType(JobType jobType);

    List<JobPosting> findByExperienceLevel(ExperienceLevel experienceLevel);

    List<JobPosting> findByTitleContainingIgnoreCase(String title);
}
