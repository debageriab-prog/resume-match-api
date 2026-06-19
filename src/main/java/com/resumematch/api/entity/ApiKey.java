package com.resumematch.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An encrypted LLM provider API key.
 * <p>
 * Only ciphertext is stored here; plaintext never persists at rest.
 * The {@code provider} primary key matches the provider names used by the LLM router
 * (e.g. {@code openai}, {@code anthropic}, {@code google}, {@code deepseek}, {@code ollama}).
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "ciphertext", nullable = false, columnDefinition = "TEXT")
    private String ciphertext;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = Instant.now();
    }
}
