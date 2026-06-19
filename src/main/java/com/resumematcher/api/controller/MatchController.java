package com.resumematcher.api.controller;

import com.resumematcher.api.entity.Match;
import com.resumematcher.api.entity.enums.MatchStatus;
import com.resumematcher.api.service.MatchService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Manage resume-to-job matching results")
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    @Operation(summary = "Get all matches")
    public ResponseEntity<List<Match>> getAll() {
        return ResponseEntity.ok(matchService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get match by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Match found"),
        @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public ResponseEntity<Match> getById(
            @Parameter(description = "Match ID") @PathVariable Long id) {
        return ResponseEntity.ok(matchService.findById(id));
    }

    @GetMapping("/resume/{resumeId}")
    @Operation(summary = "Get all matches for a resume (sorted by score)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matches retrieved"),
        @ApiResponse(responseCode = "404", description = "Resume not found")
    })
    public ResponseEntity<List<Match>> getByResumeId(
            @Parameter(description = "Resume ID") @PathVariable Long resumeId) {
        return ResponseEntity.ok(matchService.findByResumeId(resumeId));
    }

    @GetMapping("/job-posting/{jobPostingId}")
    @Operation(summary = "Get all matches for a job posting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matches retrieved"),
        @ApiResponse(responseCode = "404", description = "Job posting not found")
    })
    public ResponseEntity<List<Match>> getByJobPostingId(
            @Parameter(description = "Job posting ID") @PathVariable Long jobPostingId) {
        return ResponseEntity.ok(matchService.findByJobPostingId(jobPostingId));
    }

    @PostMapping("/resume/{resumeId}/job-posting/{jobPostingId}")
    @Operation(summary = "Create a match between a resume and a job posting")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Match created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Resume or job posting not found"),
        @ApiResponse(responseCode = "409", description = "Match already exists")
    })
    public ResponseEntity<Match> create(
            @Parameter(description = "Resume ID") @PathVariable Long resumeId,
            @Parameter(description = "Job posting ID") @PathVariable Long jobPostingId,
            @Valid @RequestBody Match match) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.create(resumeId, jobPostingId, match));
    }

    @PatchMapping("/{id}/score")
    @Operation(summary = "Update the score and analysis of a match")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Score updated"),
        @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public ResponseEntity<Match> updateScore(
            @Parameter(description = "Match ID") @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        Double score = payload.get("score") != null ? Double.valueOf(payload.get("score").toString()) : null;
        String analysisDetails = (String) payload.get("analysisDetails");
        return ResponseEntity.ok(matchService.updateScore(id, score, analysisDetails));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update the status of a match")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public ResponseEntity<Match> updateStatus(
            @Parameter(description = "Match ID") @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        MatchStatus status = MatchStatus.valueOf(payload.get("status"));
        return ResponseEntity.ok(matchService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a match")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Match deleted"),
        @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Match ID") @PathVariable Long id) {
        matchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
