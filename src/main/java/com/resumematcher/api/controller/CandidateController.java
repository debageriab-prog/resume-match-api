package com.resumematcher.api.controller;

import com.resumematcher.api.entity.Candidate;
import com.resumematcher.api.service.CandidateService;
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
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates", description = "Manage job seeker profiles")
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping
    @Operation(summary = "Get all candidates")
    public ResponseEntity<List<Candidate>> getAll() {
        return ResponseEntity.ok(candidateService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get candidate by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Candidate found"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<Candidate> getById(
            @Parameter(description = "Candidate ID") @PathVariable Long id) {
        return ResponseEntity.ok(candidateService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Candidate created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<Candidate> create(@Valid @RequestBody Candidate candidate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateService.create(candidate));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Candidate updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Candidate not found"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public ResponseEntity<Candidate> update(
            @Parameter(description = "Candidate ID") @PathVariable Long id,
            @Valid @RequestBody Candidate candidate) {
        return ResponseEntity.ok(candidateService.update(id, candidate));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a candidate")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Candidate deleted"),
        @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Candidate ID") @PathVariable Long id) {
        candidateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
