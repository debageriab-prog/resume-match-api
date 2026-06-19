package com.resumematch.api.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A tailoring result linking an original resume, a tailored resume variant, and a job.
 * <p>
 * {@code improvements} is a JSON array of {@code ImprovementSuggestion} objects
 * produced by the LLM pipeline. Each entry contains a {@code suggestion} string
 * and an optional {@code lineNumber}.
 */
@Entity
@Table(
    name = "improvements",
    indexes = {
        @Index(name = "idx_improvements_tailored_resume_id", columnList = "tailored_resume_id"),
        @Index(name = "idx_improvements_job_id", columnList = "job_id"),
        @Index(name = "idx_improvements_original_resume_id", columnList = "original_resume_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Improvement {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private String requestId;

    @Column(name = "original_resume_id", nullable = false)
    private String originalResumeId;

    @Column(name = "tailored_resume_id", nullable = false)
    private String tailoredResumeId;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Type(JsonType.class)
    @Column(name = "improvements", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> improvements = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (improvements == null) {
            improvements = new ArrayList<>();
        }
    }
}
