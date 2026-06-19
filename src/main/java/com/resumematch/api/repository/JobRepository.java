package com.resumematch.api.repository;

import com.resumematch.api.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByResumeId(String resumeId);
}
