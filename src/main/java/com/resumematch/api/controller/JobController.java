package com.resumematch.api.controller;

import com.resumematch.api.dto.JobDTO;
import com.resumematch.api.service.JobService;
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
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "CRUD operations for job description documents")
public class JobController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "List all job descriptions")
    public ResponseEntity<List<JobDTO>> findAll() {
        return ResponseEntity.ok(jobService.findAll());
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a job description by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job found"),
        @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    })
    public ResponseEntity<JobDTO> findById(
            @Parameter(description = "Job UUID", required = true)
            @PathVariable String jobId) {
        return ResponseEntity.ok(jobService.findById(jobId));
    }

    @GetMapping("/by-resume/{resumeId}")
    @Operation(summary = "List job descriptions linked to a specific resume")
    public ResponseEntity<List<JobDTO>> findByResumeId(
            @Parameter(description = "Resume UUID", required = true)
            @PathVariable String resumeId) {
        return ResponseEntity.ok(jobService.findByResumeId(resumeId));
    }

    @PostMapping
    @Operation(summary = "Create a job description")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Job created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<JobDTO> create(@Valid @RequestBody JobDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.create(dto));
    }

    @PutMapping("/{jobId}")
    @Operation(summary = "Update a job description")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job updated"),
        @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    })
    public ResponseEntity<JobDTO> update(
            @Parameter(description = "Job UUID", required = true)
            @PathVariable String jobId,
            @Valid @RequestBody JobDTO dto) {
        return ResponseEntity.ok(jobService.update(jobId, dto));
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete a job description")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Job deleted"),
        @ApiResponse(responseCode = "404", description = "Job not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Job UUID", required = true)
            @PathVariable String jobId) {
        jobService.delete(jobId);
        return ResponseEntity.noContent().build();
    }
}
