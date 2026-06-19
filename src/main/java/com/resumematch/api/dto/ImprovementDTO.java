package com.resumematch.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "Tailoring result linking an original resume, a tailored resume, and a job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImprovementDTO {

    @Schema(description = "Unique request identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440002")
    private String requestId;

    @NotBlank(message = "originalResumeId is required")
    @Schema(description = "ID of the original (master) resume", required = true)
    private String originalResumeId;

    @NotBlank(message = "tailoredResumeId is required")
    @Schema(description = "ID of the tailored resume variant", required = true)
    private String tailoredResumeId;

    @NotBlank(message = "jobId is required")
    @Schema(description = "ID of the job this improvement targets", required = true)
    private String jobId;

    @Schema(
        description = "List of improvement suggestions produced by the LLM pipeline. " +
                      "Each entry contains a 'suggestion' string and an optional 'lineNumber'.",
        example = "[{\"suggestion\": \"Add Docker to skills section\", \"lineNumber\": 12}]"
    )
    @Builder.Default
    private List<Map<String, Object>> improvements = new ArrayList<>();

    @Schema(description = "Creation timestamp (ISO-8601)")
    private Instant createdAt;
}
