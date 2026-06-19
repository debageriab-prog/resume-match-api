package com.resumematch.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Schema(description = "Job description document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDTO {

    @Schema(description = "Unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440001")
    private String jobId;

    @NotBlank(message = "content is required")
    @Schema(description = "Raw job description text", required = true)
    private String content;

    @Schema(description = "ID of the resume this job was created for")
    private String resumeId;

    @Schema(description = "Creation timestamp (ISO-8601)")
    private Instant createdAt;

    @Schema(
        description = "Dynamic pipeline metadata: job_keywords, company, role, preview_hash, etc.",
        example = "{\"company\": \"Acme Corp\", \"role\": \"Backend Engineer\"}"
    )
    @Builder.Default
    private Map<String, Object> metadataJson = new HashMap<>();
}
