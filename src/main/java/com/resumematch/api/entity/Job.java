package com.resumematch.api.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A job description document.
 * <p>
 * Core stable columns ({@code jobId}, {@code content}, {@code resumeId}) are first-class.
 * Dynamic pipeline fields such as {@code job_keywords}, {@code company}, {@code role},
 * and hash values live inside {@code metadataJson} and are flattened on read.
 */
@Entity
@Table(
    name = "jobs",
    indexes = {
        @Index(name = "idx_jobs_resume_id", columnList = "resume_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private String jobId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "resume_id")
    private String resumeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Type(JsonType.class)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadataJson = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (metadataJson == null) {
            metadataJson = new HashMap<>();
        }
    }
}
