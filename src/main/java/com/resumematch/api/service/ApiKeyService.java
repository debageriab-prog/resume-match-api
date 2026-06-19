package com.resumematch.api.service;

import com.resumematch.api.dto.ApiKeyDTO;
import com.resumematch.api.entity.ApiKey;
import com.resumematch.api.exception.ResourceNotFoundException;
import com.resumematch.api.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    @Transactional(readOnly = true)
    public List<ApiKeyDTO> findAll() {
        return apiKeyRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public ApiKeyDTO findByProvider(String provider) {
        return apiKeyRepository.findById(provider)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", provider));
    }

    @Transactional
    public ApiKeyDTO save(ApiKeyDTO dto) {
        ApiKey entity = apiKeyRepository.findById(dto.getProvider())
            .orElse(new ApiKey());
        entity.setProvider(dto.getProvider());
        entity.setCiphertext(dto.getCiphertext());
        return toDTO(apiKeyRepository.save(entity));
    }

    @Transactional
    public void delete(String provider) {
        if (!apiKeyRepository.existsById(provider)) {
            throw new ResourceNotFoundException("ApiKey", provider);
        }
        apiKeyRepository.deleteById(provider);
    }

    private ApiKeyDTO toDTO(ApiKey entity) {
        return ApiKeyDTO.builder()
            .provider(entity.getProvider())
            .ciphertext(entity.getCiphertext())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
