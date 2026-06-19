package com.resumematcher.api.repository;

import com.resumematcher.api.entity.Skill;
import com.resumematcher.api.entity.enums.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findByCategory(SkillCategory category);

    boolean existsByNameIgnoreCase(String name);
}
