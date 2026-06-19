package com.resumematcher.api.controller;

import com.resumematcher.api.entity.Experience;
import com.resumematcher.api.service.ExperienceService;
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
@RequestMapping("/api/experiences")
@RequiredArgsConstructor
@Tag(name = "Experiences", description = "Manage candidate work experiences")
public class ExperienceController {

    private final ExperienceService experienceService;

    @GetMapping
    @Operation(summary = "Get all experiences")
    public ResponseEntity<List<Experience>> getAll() {
        return ResponseEntity.ok(experienceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get experience by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Experience found"),
        @ApiResponse(responseCode = "404", description = "Experience not found")
    })
    public ResponseEntity<Experience> getById(
            @Parameter(description = "Experience ID") @PathVariable Long id) {
        return ResponseEntity.ok(experienceService.findById(id));
    }

    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Get all experiences for a candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Experiences retrieved"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<List<Experience>> getByCandidateId(
            @Parameter(description = "Candidate ID") @PathVariable Long candidateId) {
        return ResponseEntity.ok(experienceService.findByCandidateId(candidateId));
    }

    @PostMapping("/candidate/{candidateId}")
    @Operation(summary = "Add experience to a candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Experience created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<Experience> create(
            @Parameter(description = "Candidate ID") @PathVariable Long candidateId,
            @Valid @RequestBody Experience experience) {
        return ResponseEntity.status(HttpStatus.CREATED).body(experienceService.create(candidateId, experience));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an experience")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Experience updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Experience not found")
    })
    public ResponseEntity<Experience> update(
            @Parameter(description = "Experience ID") @PathVariable Long id,
            @Valid @RequestBody Experience experience) {
        return ResponseEntity.ok(experienceService.update(id, experience));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an experience")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Experience deleted"),
        @ApiResponse(responseCode = "404", description = "Experience not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Experience ID") @PathVariable Long id) {
        experienceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
