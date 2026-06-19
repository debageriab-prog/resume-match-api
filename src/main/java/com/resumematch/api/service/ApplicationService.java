package com.resumematch.api.service;

import com.resumematch.api.dto.ApplicationDTO;
import com.resumematch.api.entity.Application;
import com.resumematch.api.exception.ConflictException;
import com.resumematch.api.exception.ResourceNotFoundException;
import com.resumematch.api.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<ApplicationDTO> findAll() {
        return applicationRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public ApplicationDTO findById(String applicationId) {
        return applicationRepository.findById(applicationId)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> findByStatus(String status) {
        return applicationRepository.findByStatusOrderByPositionAsc(status).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> findByJobId(String jobId) {
        return applicationRepository.findByJobId(jobId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> findByResumeId(String resumeId) {
        return applicationRepository.findByResumeId(resumeId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public ApplicationDTO create(ApplicationDTO dto) {
        if (applicationRepository.existsByJobIdAndResumeId(dto.getJobId(), dto.getResumeId())) {
            throw new ConflictException(
                "An application for job '" + dto.getJobId() + "' and resume '" + dto.getResumeId() + "' already exists");
        }
        String id = dto.getApplicationId() != null ? dto.getApplicationId() : UUID.randomUUID().toString();
        Application entity = toEntity(dto);
        entity.setApplicationId(id);
        return toDTO(applicationRepository.save(entity));
    }

    @Transactional
    public ApplicationDTO update(String applicationId, ApplicationDTO dto) {
        Application existing = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        existing.setJobId(dto.getJobId());
        existing.setResumeId(dto.getResumeId());
        existing.setMasterResumeId(dto.getMasterResumeId());
        existing.setStatus(dto.getStatus() != null ? dto.getStatus() : existing.getStatus());
        existing.setCompany(dto.getCompany());
        existing.setRole(dto.getRole());
        existing.setAppliedAt(dto.getAppliedAt());
        existing.setNotes(dto.getNotes());
        existing.setPosition(dto.getPosition() != null ? dto.getPosition() : existing.getPosition());

        return toDTO(applicationRepository.save(existing));
    }

    @Transactional
    public void delete(String applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application", applicationId);
        }
        applicationRepository.deleteById(applicationId);
    }

    private ApplicationDTO toDTO(Application entity) {
        return ApplicationDTO.builder()
            .applicationId(entity.getApplicationId())
            .jobId(entity.getJobId())
            .resumeId(entity.getResumeId())
            .masterResumeId(entity.getMasterResumeId())
            .status(entity.getStatus())
            .company(entity.getCompany())
            .role(entity.getRole())
            .appliedAt(entity.getAppliedAt())
            .notes(entity.getNotes())
            .position(entity.getPosition())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private Application toEntity(ApplicationDTO dto) {
        return Application.builder()
            .applicationId(dto.getApplicationId())
            .jobId(dto.getJobId())
            .resumeId(dto.getResumeId())
            .masterResumeId(dto.getMasterResumeId())
            .status(dto.getStatus() != null ? dto.getStatus() : "applied")
            .company(dto.getCompany())
            .role(dto.getRole())
            .appliedAt(dto.getAppliedAt())
            .notes(dto.getNotes())
            .position(dto.getPosition() != null ? dto.getPosition() : 0)
            .build();
    }
}
