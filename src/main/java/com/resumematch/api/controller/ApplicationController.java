package com.resumematch.api.controller;

import com.resumematch.api.dto.ApplicationDTO;
import com.resumematch.api.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "CRUD operations for Kanban job-application tracker cards")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    @Operation(summary = "List all applications")
    public ResponseEntity<List<ApplicationDTO>> findAll() {
        return ResponseEntity.ok(applicationService.findAll());
    }

    @GetMapping("/{applicationId}")
    @Operation(summary = "Get an application by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application found"),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    })
    public ResponseEntity<ApplicationDTO> findById(
            @Parameter(description = "Application UUID", required = true)
            @PathVariable String applicationId) {
        return ResponseEntity.ok(applicationService.findById(applicationId));
    }

    @GetMapping("/by-status/{status}")
    @Operation(
        summary = "List applications by status",
        description = "Returns applications ordered by position asc. " +
                      "Valid statuses: applied, screening, interviewing, offered, rejected"
    )
    public ResponseEntity<List<ApplicationDTO>> findByStatus(
            @Parameter(description = "Application status", example = "applied", required = true)
            @PathVariable String status) {
        return ResponseEntity.ok(applicationService.findByStatus(status));
    }

    @GetMapping("/by-job/{jobId}")
    @Operation(summary = "List applications linked to a specific job")
    public ResponseEntity<List<ApplicationDTO>> findByJobId(
            @Parameter(description = "Job UUID", required = true)
            @PathVariable String jobId) {
        return ResponseEntity.ok(applicationService.findByJobId(jobId));
    }

    @GetMapping("/by-resume/{resumeId}")
    @Operation(summary = "List applications linked to a specific resume")
    public ResponseEntity<List<ApplicationDTO>> findByResumeId(
            @Parameter(description = "Resume UUID", required = true)
            @PathVariable String resumeId) {
        return ResponseEntity.ok(applicationService.findByResumeId(resumeId));
    }

    @PostMapping
    @Operation(
        summary = "Create an application",
        description = "Creates a new Kanban card. Returns 409 if a card for the same (job, resume) pair already exists."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Application created"),
        @ApiResponse(responseCode = "409", description = "Duplicate (job, resume) pair", content = @Content),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<ApplicationDTO> create(@Valid @RequestBody ApplicationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.create(dto));
    }

    @PutMapping("/{applicationId}")
    @Operation(summary = "Update an application")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Application updated"),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    })
    public ResponseEntity<ApplicationDTO> update(
            @Parameter(description = "Application UUID", required = true)
            @PathVariable String applicationId,
            @Valid @RequestBody ApplicationDTO dto) {
        return ResponseEntity.ok(applicationService.update(applicationId, dto));
    }

    @DeleteMapping("/{applicationId}")
    @Operation(summary = "Delete an application")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Application deleted"),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Application UUID", required = true)
            @PathVariable String applicationId) {
        applicationService.delete(applicationId);
        return ResponseEntity.noContent().build();
    }
}
