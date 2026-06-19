package com.resumematcher.api.repository;

import com.resumematcher.api.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByCandidateId(Long candidateId);

    List<Resume> findByCandidateIdAndIsActive(Long candidateId, Boolean isActive);
}
