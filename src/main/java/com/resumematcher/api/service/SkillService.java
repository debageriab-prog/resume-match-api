package com.resumematcher.api.service;

import com.resumematcher.api.entity.Skill;
import com.resumematcher.api.entity.enums.SkillCategory;
import com.resumematcher.api.exception.DuplicateResourceException;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillService {

    private final SkillRepository skillRepository;

    public List<Skill> findAll() {
        return skillRepository.findAll();
    }

    public Skill findById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", id));
    }

    public List<Skill> findByCategory(SkillCategory category) {
        return skillRepository.findByCategory(category);
    }

    @Transactional
    public Skill create(Skill skill) {
        if (skillRepository.existsByNameIgnoreCase(skill.getName())) {
            throw new DuplicateResourceException("Skill already exists with name: " + skill.getName());
        }
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill update(Long id, Skill updatedSkill) {
        Skill existing = findById(id);

        if (!existing.getName().equalsIgnoreCase(updatedSkill.getName())
                && skillRepository.existsByNameIgnoreCase(updatedSkill.getName())) {
            throw new DuplicateResourceException("Skill name already in use: " + updatedSkill.getName());
        }

        existing.setName(updatedSkill.getName());
        existing.setCategory(updatedSkill.getCategory());
        existing.setDescription(updatedSkill.getDescription());
        return skillRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill", id);
        }
        skillRepository.deleteById(id);
    }
}
