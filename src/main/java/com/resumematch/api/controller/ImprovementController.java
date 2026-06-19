package com.resumematch.api.controller;

import com.resumematch.api.dto.ImprovementDTO;
import com.resumematch.api.service.ImprovementService;
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
@RequestMapping("/api/v1/improvements")
@RequiredArgsConstructor
@Tag(name = "Improvements", description = "CRUD operations for LLM-generated resume tailoring results")
public class ImprovementController {

    private final ImprovementService improvementService;

    @GetMapping
    @Operation(summary = "List all improvement records")
    public ResponseEntity<List<ImprovementDTO>> findAll() {
        return ResponseEntity.ok(improvementService.findAll());
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get an improvement record by request ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Improvement found"),
        @ApiResponse(responseCode = "404", description = "Improvement not found", content = @Content)
    })
    public ResponseEntity<ImprovementDTO> findById(
            @Parameter(description = "Improvement request UUID", required = true)
            @PathVariable String requestId) {
        return ResponseEntity.ok(improvementService.findById(requestId));
    }

    @GetMapping("/by-resume/{resumeId}")
    @Operation(summary = "List improvements targeting a specific original resume")
    public ResponseEntity<List<ImprovementDTO>> findByOriginalResumeId(
            @Parameter(description = "Original resume UUID", required = true)
            @PathVariable String resumeId) {
        return ResponseEntity.ok(improvementService.findByOriginalResumeId(resumeId));
    }

    @GetMapping("/by-job/{jobId}")
    @Operation(summary = "List improvements targeting a specific job")
    public ResponseEntity<List<ImprovementDTO>> findByJobId(
            @Parameter(description = "Job UUID", required = true)
            @PathVariable String jobId) {
        return ResponseEntity.ok(improvementService.findByJobId(jobId));
    }

    @PostMapping
    @Operation(summary = "Create an improvement record")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Improvement created"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<ImprovementDTO> create(@Valid @RequestBody ImprovementDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(improvementService.create(dto));
    }

    @PutMapping("/{requestId}")
    @Operation(summary = "Update an improvement record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Improvement updated"),
        @ApiResponse(responseCode = "404", description = "Improvement not found", content = @Content)
    })
    public ResponseEntity<ImprovementDTO> update(
            @Parameter(description = "Improvement request UUID", required = true)
            @PathVariable String requestId,
            @Valid @RequestBody ImprovementDTO dto) {
        return ResponseEntity.ok(improvementService.update(requestId, dto));
    }

    @DeleteMapping("/{requestId}")
    @Operation(summary = "Delete an improvement record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Improvement deleted"),
        @ApiResponse(responseCode = "404", description = "Improvement not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Improvement request UUID", required = true)
            @PathVariable String requestId) {
        improvementService.delete(requestId);
        return ResponseEntity.noContent().build();
    }
}
