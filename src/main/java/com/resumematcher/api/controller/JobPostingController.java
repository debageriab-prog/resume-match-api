package com.resumematcher.api.controller;

import com.resumematcher.api.entity.JobPosting;
import com.resumematcher.api.entity.enums.JobStatus;
import com.resumematcher.api.service.JobPostingService;
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
@RequestMapping("/api/job-postings")
@RequiredArgsConstructor
@Tag(name = "Job Postings", description = "Manage job postings")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @GetMapping
    @Operation(summary = "Get all job postings")
    public ResponseEntity<List<JobPosting>> getAll(
            @Parameter(description = "Filter by status") @RequestParam(required = false) JobStatus status) {
        if (status != null) {
            return ResponseEntity.ok(jobPostingService.findByStatus(status));
        }
        return ResponseEntity.ok(jobPostingService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job posting by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job posting found"),
        @ApiResponse(responseCode = "404", description = "Job posting not found")
    })
    public ResponseEntity<JobPosting> getById(
            @Parameter(description = "Job posting ID") @PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.findById(id));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get all job postings for a company")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job postings retrieved"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<List<JobPosting>> getByCompanyId(
            @Parameter(description = "Company ID") @PathVariable Long companyId) {
        return ResponseEntity.ok(jobPostingService.findByCompanyId(companyId));
    }

    @PostMapping("/company/{companyId}")
    @Operation(summary = "Create a job posting for a company")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Job posting created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<JobPosting> create(
            @Parameter(description = "Company ID") @PathVariable Long companyId,
            @Valid @RequestBody JobPosting jobPosting) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobPostingService.create(companyId, jobPosting));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job posting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Job posting updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Job posting not found")
    })
    public ResponseEntity<JobPosting> update(
            @Parameter(description = "Job posting ID") @PathVariable Long id,
            @Valid @RequestBody JobPosting jobPosting) {
        return ResponseEntity.ok(jobPostingService.update(id, jobPosting));
    }

    @PostMapping("/{id}/skills")
    @Operation(summary = "Add skills to a job posting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Skills added"),
        @ApiResponse(responseCode = "404", description = "Job posting or skill not found")
    })
    public ResponseEntity<JobPosting> addSkills(
            @Parameter(description = "Job posting ID") @PathVariable Long id,
            @RequestBody List<Long> skillIds) {
        return ResponseEntity.ok(jobPostingService.addSkills(id, skillIds));
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    @Operation(summary = "Remove a skill from a job posting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Skill removed"),
        @ApiResponse(responseCode = "404", description = "Job posting or skill not found")
    })
    public ResponseEntity<JobPosting> removeSkill(
            @Parameter(description = "Job posting ID") @PathVariable Long id,
            @Parameter(description = "Skill ID") @PathVariable Long skillId) {
        return ResponseEntity.ok(jobPostingService.removeSkill(id, skillId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job posting")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Job posting deleted"),
        @ApiResponse(responseCode = "404", description = "Job posting not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Job posting ID") @PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
