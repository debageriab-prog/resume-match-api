package com.resumematcher.api.controller;

import com.resumematcher.api.entity.Education;
import com.resumematcher.api.service.EducationService;
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
@RequestMapping("/api/educations")
@RequiredArgsConstructor
@Tag(name = "Educations", description = "Manage candidate educational backgrounds")
public class EducationController {

    private final EducationService educationService;

    @GetMapping
    @Operation(summary = "Get all education records")
    public ResponseEntity<List<Education>> getAll() {
        return ResponseEntity.ok(educationService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get education record by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Education found"),
        @ApiResponse(responseCode = "404", description = "Education not found")
    })
    public ResponseEntity<Education> getById(
            @Parameter(description = "Education ID") @PathVariable Long id) {
        return ResponseEntity.ok(educationService.findById(id));
    }

    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Get all education records for a candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Education records retrieved"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<List<Education>> getByCandidateId(
            @Parameter(description = "Candidate ID") @PathVariable Long candidateId) {
        return ResponseEntity.ok(educationService.findByCandidateId(candidateId));
    }

    @PostMapping("/candidate/{candidateId}")
    @Operation(summary = "Add education record to a candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Education record created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<Education> create(
            @Parameter(description = "Candidate ID") @PathVariable Long candidateId,
            @Valid @RequestBody Education education) {
        return ResponseEntity.status(HttpStatus.CREATED).body(educationService.create(candidateId, education));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an education record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Education updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Education not found")
    })
    public ResponseEntity<Education> update(
            @Parameter(description = "Education ID") @PathVariable Long id,
            @Valid @RequestBody Education education) {
        return ResponseEntity.ok(educationService.update(id, education));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an education record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Education deleted"),
        @ApiResponse(responseCode = "404", description = "Education not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Education ID") @PathVariable Long id) {
        educationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
