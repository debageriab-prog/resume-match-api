package com.resumematch.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Resume document — either a master resume or a tailored variant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDTO {

    @Schema(description = "Unique identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String resumeId;

    @NotBlank(message = "content is required")
    @Schema(description = "Raw markdown content of the resume", required = true)
    private String content;

    @Schema(description = "Content format (md, html, txt)", example = "md", defaultValue = "md")
    @Builder.Default
    private String contentType = "md";

    @Schema(description = "Original upload filename")
    private String filename;

    @Schema(description = "True when this is the single master resume", defaultValue = "false")
    @Builder.Default
    private Boolean isMaster = false;

    @Schema(description = "ID of the master resume this tailored resume was derived from")
    private String parentId;

    @Schema(description = "Structured data extracted from the resume content (JSON object)")
    private Map<String, Object> processedData;

    @Schema(
        description = "Async processing pipeline state",
        allowableValues = {"pending", "processing", "ready", "failed"},
        defaultValue = "pending"
    )
    @Builder.Default
    private String processingStatus = "pending";

    @Schema(description = "Cover letter text associated with this resume")
    private String coverLetter;

    @Schema(description = "Outreach message associated with this resume")
    private String outreachMessage;

    @Schema(description = "Human-readable title for the resume")
    private String title;

    @Schema(description = "Saved original markdown before tailoring was applied")
    private String originalMarkdown;

    @Schema(description = "Creation timestamp (ISO-8601)")
    private Instant createdAt;

    @Schema(description = "Last-update timestamp (ISO-8601)")
    private Instant updatedAt;
}
