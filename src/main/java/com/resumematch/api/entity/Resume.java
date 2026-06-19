package com.resumematch.api.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

/**
 * A resume document — either a master resume or a tailored variant derived from one.
 * <p>
 * {@code isMaster} flags the single canonical resume; at most one row may have this set.
 * {@code parentId} links tailored resumes back to the master they were derived from.
 * {@code processedData} holds the structured JSON parsed from the raw markdown content.
 * {@code processingStatus} tracks the async pipeline state: pending → processing → ready | failed.
 */
@Entity
@Table(
    name = "resumes",
    indexes = {
        @Index(name = "idx_resumes_is_master", columnList = "is_master"),
        @Index(name = "idx_resumes_parent_id", columnList = "parent_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @Column(name = "resume_id", nullable = false, updatable = false)
    private String resumeId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_type", nullable = false)
    @Builder.Default
    private String contentType = "md";

    @Column(name = "filename")
    private String filename;

    @Column(name = "is_master", nullable = false)
    @Builder.Default
    private Boolean isMaster = false;

    @Column(name = "parent_id")
    private String parentId;

    @Type(JsonType.class)
    @Column(name = "processed_data", columnDefinition = "jsonb")
    private Map<String, Object> processedData;

    @Column(name = "processing_status", nullable = false)
    @Builder.Default
    private String processingStatus = "pending";

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "outreach_message", columnDefinition = "TEXT")
    private String outreachMessage;

    @Column(name = "title")
    private String title;

    @Column(name = "original_markdown", columnDefinition = "TEXT")
    private String originalMarkdown;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
