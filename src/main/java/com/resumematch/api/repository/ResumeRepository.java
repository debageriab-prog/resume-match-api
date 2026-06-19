package com.resumematch.api.repository;

import com.resumematch.api.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {

    Optional<Resume> findByIsMasterTrue();

    List<Resume> findByParentId(String parentId);

    List<Resume> findByProcessingStatus(String processingStatus);

    boolean existsByIsMasterTrue();
}
