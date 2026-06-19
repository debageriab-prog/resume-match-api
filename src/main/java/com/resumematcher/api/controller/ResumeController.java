package com.resumematcher.api.controller;

import com.resumematcher.api.entity.Resume;
import com.resumematcher.api.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes", description = "Manage candidate resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    @Operation(summary = "Get all resumes")
    public ResponseEntity<List<Resume>> getAll() {
        return ResponseEntity.ok(resumeService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resume by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resume found"),
        @ApiResponse(responseCode = "404", description = "Resume not found")
    })
    public ResponseEntity<Resume> getById(
            @Parameter(description = "Resume ID") @PathVariable Long id) {
        return ResponseEntity.ok(resumeService.findById(id));
    }

    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Get all resumes for a candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumes retrieved"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<List<Resume>> getByCandidateId(
            @Parameter(description = "Candidate ID") @PathVariable Long candidateId) {
        return ResponseEntity.ok(resumeService.findByCandidateId(candidateId));
    }

    @PostMapping("/candidate/{candidateId}")
    @Operation(summary = "Create a resume for a candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Resume created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<Resume> create(
            @Parameter(description = "Candidate ID") @PathVariable Long candidateId,
            @Valid @RequestBody Resume resume) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.create(candidateId, resume));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a resume")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resume updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Resume not found")
    })
    public ResponseEntity<Resume> update(
            @Parameter(description = "Resume ID") @PathVariable Long id,
            @Valid @RequestBody Resume resume) {
        return ResponseEntity.ok(resumeService.update(id, resume));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a resume")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Resume deleted"),
        @ApiResponse(responseCode = "404", description = "Resume not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Resume ID") @PathVariable Long id) {
        resumeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
