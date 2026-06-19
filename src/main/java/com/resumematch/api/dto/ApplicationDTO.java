package com.resumematch.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Schema(description = "Kanban application-tracker card representing a job application")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDTO {

    @Schema(description = "Unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440003")
    private String applicationId;

    @NotBlank(message = "jobId is required")
    @Schema(description = "ID of the job this application targets", required = true)
    private String jobId;

    @NotBlank(message = "resumeId is required")
    @Schema(description = "ID of the tailored resume used for this application", required = true)
    private String resumeId;

    @Schema(description = "ID of the master resume this tailored resume descends from")
    private String masterResumeId;

    @Schema(
        description = "Application lifecycle status",
        allowableValues = {"applied", "screening", "interviewing", "offered", "rejected"},
        defaultValue = "applied"
    )
    @Builder.Default
    private String status = "applied";

    @Schema(description = "Company name", example = "Acme Corp")
    private String company;

    @Schema(description = "Role or job title", example = "Senior Backend Engineer")
    private String role;

    @Schema(description = "When the application was submitted (ISO-8601)")
    private Instant appliedAt;

    @Schema(description = "Free-form notes about the application")
    private String notes;

    @Schema(description = "Display order within the Kanban column (0 = first)", defaultValue = "0")
    @Builder.Default
    private Integer position = 0;

    @Schema(description = "Creation timestamp (ISO-8601)")
    private Instant createdAt;

    @Schema(description = "Last-update timestamp (ISO-8601)")
    private Instant updatedAt;
}
