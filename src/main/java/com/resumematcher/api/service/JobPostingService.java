package com.resumematcher.api.service;

import com.resumematcher.api.entity.Company;
import com.resumematcher.api.entity.JobPosting;
import com.resumematcher.api.entity.Skill;
import com.resumematcher.api.entity.enums.JobStatus;
import com.resumematcher.api.exception.ResourceNotFoundException;
import com.resumematcher.api.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final CompanyService companyService;
    private final SkillService skillService;

    public List<JobPosting> findAll() {
        return jobPostingRepository.findAll();
    }

    public JobPosting findById(Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobPosting", id));
    }

    public List<JobPosting> findByCompanyId(Long companyId) {
        companyService.findById(companyId);
        return jobPostingRepository.findByCompanyId(companyId);
    }

    public List<JobPosting> findByStatus(JobStatus status) {
        return jobPostingRepository.findByStatus(status);
    }

    @Transactional
    public JobPosting create(Long companyId, JobPosting jobPosting) {
        Company company = companyService.findById(companyId);
        jobPosting.setCompany(company);
        return jobPostingRepository.save(jobPosting);
    }

    @Transactional
    public JobPosting update(Long id, JobPosting updatedJobPosting) {
        JobPosting existing = findById(id);
        existing.setTitle(updatedJobPosting.getTitle());
        existing.setDescription(updatedJobPosting.getDescription());
        existing.setRequirements(updatedJobPosting.getRequirements());
        existing.setLocation(updatedJobPosting.getLocation());
        existing.setJobType(updatedJobPosting.getJobType());
        existing.setStatus(updatedJobPosting.getStatus());
        existing.setExperienceLevel(updatedJobPosting.getExperienceLevel());
        existing.setSalaryMin(updatedJobPosting.getSalaryMin());
        existing.setSalaryMax(updatedJobPosting.getSalaryMax());
        existing.setCurrency(updatedJobPosting.getCurrency());
        existing.setExpiresAt(updatedJobPosting.getExpiresAt());
        return jobPostingRepository.save(existing);
    }

    @Transactional
    public JobPosting addSkills(Long jobPostingId, List<Long> skillIds) {
        JobPosting jobPosting = findById(jobPostingId);
        Set<Skill> skills = skillIds.stream()
                .map(skillService::findById)
                .collect(Collectors.toSet());
        jobPosting.getSkills().addAll(skills);
        return jobPostingRepository.save(jobPosting);
    }

    @Transactional
    public JobPosting removeSkill(Long jobPostingId, Long skillId) {
        JobPosting jobPosting = findById(jobPostingId);
        Skill skill = skillService.findById(skillId);
        jobPosting.getSkills().remove(skill);
        return jobPostingRepository.save(jobPosting);
    }

    @Transactional
    public void delete(Long id) {
        if (!jobPostingRepository.existsById(id)) {
            throw new ResourceNotFoundException("JobPosting", id);
        }
        jobPostingRepository.deleteById(id);
    }
}
