package com.resumematcher.api.repository;

import com.resumematcher.api.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    List<Experience> findByCandidateId(Long candidateId);

    List<Experience> findByCandidateIdOrderByStartDateDesc(Long candidateId);
}
