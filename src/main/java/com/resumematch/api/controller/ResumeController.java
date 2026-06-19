package com.resumematch.api.controller;

import com.resumematch.api.dto.ResumeDTO;
import com.resumematch.api.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes", description = "CRUD operations for resume documents (master and tailored variants)")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    @Operation(summary = "List all resumes", description = "Returns all resume documents, both master and tailored variants")
    @ApiResponse(responseCode = "200", description = "List of resumes")
    public ResponseEntity<List<ResumeDTO>> findAll() {
        return ResponseEntity.ok(resumeService.findAll());
    }

    @GetMapping("/master")
    @Operation(summary = "Get the master resume", description = "Returns the single master resume")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Master resume found"),
        @ApiResponse(responseCode = "404", description = "No master resume exists", content = @Content)
    })
    public ResponseEntity<ResumeDTO> findMaster() {
        return ResponseEntity.ok(resumeService.findMaster());
    }

    @GetMapping("/{resumeId}")
    @Operation(summary = "Get a resume by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resume found"),
        @ApiResponse(responseCode = "404", description = "Resume not found", content = @Content)
    })
    public ResponseEntity<ResumeDTO> findById(
            @Parameter(description = "Resume UUID", required = true)
            @PathVariable String resumeId) {
        return ResponseEntity.ok(resumeService.findById(resumeId));
    }

    @GetMapping("/{resumeId}/tailored")
    @Operation(
        summary = "List tailored resumes derived from a master",
        description = "Returns all resumes whose parentId matches the given resumeId"
    )
    public ResponseEntity<List<ResumeDTO>> findTailored(
            @Parameter(description = "Parent (master) resume UUID", required = true)
            @PathVariable String resumeId) {
        return ResponseEntity.ok(resumeService.findByParentId(resumeId));
    }

    @PostMapping
    @Operation(summary = "Create a resume", description = "Creates a new resume document. If isMaster=true, any existing master will cause a 409 Conflict.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Resume created"),
        @ApiResponse(responseCode = "409", description = "A master resume already exists", content = @Content),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<ResumeDTO> create(
            @Valid @RequestBody ResumeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.create(dto));
    }

    @PutMapping("/{resumeId}")
    @Operation(summary = "Update a resume")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resume updated"),
        @ApiResponse(responseCode = "404", description = "Resume not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Cannot promote: another master resume exists", content = @Content)
    })
    public ResponseEntity<ResumeDTO> update(
            @Parameter(description = "Resume UUID", required = true)
            @PathVariable String resumeId,
            @Valid @RequestBody ResumeDTO dto) {
        return ResponseEntity.ok(resumeService.update(resumeId, dto));
    }

    @DeleteMapping("/{resumeId}")
    @Operation(summary = "Delete a resume")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Resume deleted"),
        @ApiResponse(responseCode = "404", description = "Resume not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Resume UUID", required = true)
            @PathVariable String resumeId) {
        resumeService.delete(resumeId);
        return ResponseEntity.noContent().build();
    }
}
