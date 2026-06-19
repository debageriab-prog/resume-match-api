package com.resumematch.api.service;

import com.resumematch.api.dto.ResumeDTO;
import com.resumematch.api.entity.Resume;
import com.resumematch.api.exception.ConflictException;
import com.resumematch.api.exception.ResourceNotFoundException;
import com.resumematch.api.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public List<ResumeDTO> findAll() {
        return resumeRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public ResumeDTO findById(String resumeId) {
        return resumeRepository.findById(resumeId)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
    }

    @Transactional(readOnly = true)
    public ResumeDTO findMaster() {
        return resumeRepository.findByIsMasterTrue()
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("No master resume found"));
    }

    @Transactional(readOnly = true)
    public List<ResumeDTO> findByParentId(String parentId) {
        return resumeRepository.findByParentId(parentId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public ResumeDTO create(ResumeDTO dto) {
        if (Boolean.TRUE.equals(dto.getIsMaster()) && resumeRepository.existsByIsMasterTrue()) {
            throw new ConflictException("A master resume already exists. Delete or demote it before promoting another.");
        }
        String id = dto.getResumeId() != null ? dto.getResumeId() : UUID.randomUUID().toString();
        Resume entity = toEntity(dto);
        entity.setResumeId(id);
        return toDTO(resumeRepository.save(entity));
    }

    @Transactional
    public ResumeDTO update(String resumeId, ResumeDTO dto) {
        Resume existing = resumeRepository.findById(resumeId)
            .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));

        if (Boolean.TRUE.equals(dto.getIsMaster())
                && !Boolean.TRUE.equals(existing.getIsMaster())
                && resumeRepository.existsByIsMasterTrue()) {
            throw new ConflictException("A master resume already exists. Delete or demote it before promoting another.");
        }

        existing.setContent(dto.getContent());
        existing.setContentType(dto.getContentType() != null ? dto.getContentType() : existing.getContentType());
        existing.setFilename(dto.getFilename());
        existing.setIsMaster(dto.getIsMaster() != null ? dto.getIsMaster() : existing.getIsMaster());
        existing.setParentId(dto.getParentId());
        existing.setProcessedData(dto.getProcessedData());
        existing.setProcessingStatus(dto.getProcessingStatus() != null ? dto.getProcessingStatus() : existing.getProcessingStatus());
        existing.setCoverLetter(dto.getCoverLetter());
        existing.setOutreachMessage(dto.getOutreachMessage());
        existing.setTitle(dto.getTitle());
        existing.setOriginalMarkdown(dto.getOriginalMarkdown());

        return toDTO(resumeRepository.save(existing));
    }

    @Transactional
    public void delete(String resumeId) {
        if (!resumeRepository.existsById(resumeId)) {
            throw new ResourceNotFoundException("Resume", resumeId);
        }
        resumeRepository.deleteById(resumeId);
    }

    private ResumeDTO toDTO(Resume entity) {
        return ResumeDTO.builder()
            .resumeId(entity.getResumeId())
            .content(entity.getContent())
            .contentType(entity.getContentType())
            .filename(entity.getFilename())
            .isMaster(entity.getIsMaster())
            .parentId(entity.getParentId())
            .processedData(entity.getProcessedData())
            .processingStatus(entity.getProcessingStatus())
            .coverLetter(entity.getCoverLetter())
            .outreachMessage(entity.getOutreachMessage())
            .title(entity.getTitle())
            .originalMarkdown(entity.getOriginalMarkdown())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private Resume toEntity(ResumeDTO dto) {
        return Resume.builder()
            .resumeId(dto.getResumeId())
            .content(dto.getContent())
            .contentType(dto.getContentType() != null ? dto.getContentType() : "md")
            .filename(dto.getFilename())
            .isMaster(dto.getIsMaster() != null ? dto.getIsMaster() : false)
            .parentId(dto.getParentId())
            .processedData(dto.getProcessedData())
            .processingStatus(dto.getProcessingStatus() != null ? dto.getProcessingStatus() : "pending")
            .coverLetter(dto.getCoverLetter())
            .outreachMessage(dto.getOutreachMessage())
            .title(dto.getTitle())
            .originalMarkdown(dto.getOriginalMarkdown())
            .build();
    }
}
