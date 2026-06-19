package com.resumematch.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A Kanban application-tracker card.
 * <p>
 * Each card represents one job application and tracks its lifecycle through
 * a set of {@code status} values (applied, screening, interviewing, offered, rejected).
 * The {@code position} field controls ordering within the Kanban column.
 * A unique constraint on ({@code jobId}, {@code resumeId}) prevents duplicate cards.
 */
@Entity
@Table(
    name = "applications",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_application_job_resume", columnNames = {"job_id", "resume_id"})
    },
    indexes = {
        @Index(name = "idx_applications_job_id", columnList = "job_id"),
        @Index(name = "idx_applications_resume_id", columnList = "resume_id"),
        @Index(name = "idx_applications_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @Column(name = "application_id", nullable = false, updatable = false)
    private String applicationId;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "resume_id", nullable = false)
    private String resumeId;

    /** Optional: links to the master resume this tailored variant descends from. */
    @Column(name = "master_resume_id")
    private String masterResumeId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "applied";

    @Column(name = "company")
    private String company;

    @Column(name = "role")
    private String role;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private Integer position = 0;

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
