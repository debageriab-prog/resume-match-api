package com.resumematch.api.service;

import com.resumematch.api.dto.ImprovementDTO;
import com.resumematch.api.entity.Improvement;
import com.resumematch.api.exception.ResourceNotFoundException;
import com.resumematch.api.repository.ImprovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImprovementService {

    private final ImprovementRepository improvementRepository;

    @Transactional(readOnly = true)
    public List<ImprovementDTO> findAll() {
        return improvementRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public ImprovementDTO findById(String requestId) {
        return improvementRepository.findById(requestId)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Improvement", requestId));
    }

    @Transactional(readOnly = true)
    public List<ImprovementDTO> findByOriginalResumeId(String resumeId) {
        return improvementRepository.findByOriginalResumeId(resumeId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ImprovementDTO> findByJobId(String jobId) {
        return improvementRepository.findByJobId(jobId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public ImprovementDTO create(ImprovementDTO dto) {
        String id = dto.getRequestId() != null ? dto.getRequestId() : UUID.randomUUID().toString();
        Improvement entity = toEntity(dto);
        entity.setRequestId(id);
        return toDTO(improvementRepository.save(entity));
    }

    @Transactional
    public ImprovementDTO update(String requestId, ImprovementDTO dto) {
        Improvement existing = improvementRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Improvement", requestId));

        existing.setOriginalResumeId(dto.getOriginalResumeId());
        existing.setTailoredResumeId(dto.getTailoredResumeId());
        existing.setJobId(dto.getJobId());
        if (dto.getImprovements() != null) {
            existing.setImprovements(dto.getImprovements());
        }

        return toDTO(improvementRepository.save(existing));
    }

    @Transactional
    public void delete(String requestId) {
        if (!improvementRepository.existsById(requestId)) {
            throw new ResourceNotFoundException("Improvement", requestId);
        }
        improvementRepository.deleteById(requestId);
    }

    private ImprovementDTO toDTO(Improvement entity) {
        return ImprovementDTO.builder()
            .requestId(entity.getRequestId())
            .originalResumeId(entity.getOriginalResumeId())
            .tailoredResumeId(entity.getTailoredResumeId())
            .jobId(entity.getJobId())
            .improvements(entity.getImprovements())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    private Improvement toEntity(ImprovementDTO dto) {
        return Improvement.builder()
            .requestId(dto.getRequestId())
            .originalResumeId(dto.getOriginalResumeId())
            .tailoredResumeId(dto.getTailoredResumeId())
            .jobId(dto.getJobId())
            .improvements(dto.getImprovements() != null ? dto.getImprovements() : new ArrayList<>())
            .build();
    }
}
