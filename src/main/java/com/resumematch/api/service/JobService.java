package com.resumematch.api.service;

import com.resumematch.api.dto.JobDTO;
import com.resumematch.api.entity.Job;
import com.resumematch.api.exception.ResourceNotFoundException;
import com.resumematch.api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public List<JobDTO> findAll() {
        return jobRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public JobDTO findById(String jobId) {
        return jobRepository.findById(jobId)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
    }

    @Transactional(readOnly = true)
    public List<JobDTO> findByResumeId(String resumeId) {
        return jobRepository.findByResumeId(resumeId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public JobDTO create(JobDTO dto) {
        String id = dto.getJobId() != null ? dto.getJobId() : UUID.randomUUID().toString();
        Job entity = toEntity(dto);
        entity.setJobId(id);
        return toDTO(jobRepository.save(entity));
    }

    @Transactional
    public JobDTO update(String jobId, JobDTO dto) {
        Job existing = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        existing.setContent(dto.getContent());
        existing.setResumeId(dto.getResumeId());
        if (dto.getMetadataJson() != null) {
            existing.setMetadataJson(dto.getMetadataJson());
        }

        return toDTO(jobRepository.save(existing));
    }

    @Transactional
    public void delete(String jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job", jobId);
        }
        jobRepository.deleteById(jobId);
    }

    private JobDTO toDTO(Job entity) {
        return JobDTO.builder()
            .jobId(entity.getJobId())
            .content(entity.getContent())
            .resumeId(entity.getResumeId())
            .createdAt(entity.getCreatedAt())
            .metadataJson(entity.getMetadataJson())
            .build();
    }

    private Job toEntity(JobDTO dto) {
        return Job.builder()
            .jobId(dto.getJobId())
            .content(dto.getContent())
            .resumeId(dto.getResumeId())
            .metadataJson(dto.getMetadataJson() != null ? dto.getMetadataJson() : new HashMap<>())
            .build();
    }
}
