package se.debageri.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.entity.ResumeMatch;
import se.debageri.api.service.ResumeMatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resume-matches")
@RequiredArgsConstructor
@Tag(name = "Resume Matches", description = "Manage resume-to-assignment match results")
public class ResumeMatchController {

	private final ResumeMatchService resumeMatchService;

	@GetMapping
	@Operation(summary = "Get all resume matches")
	public ResponseEntity<List<ResumeMatch>> getAll() {
		return ResponseEntity.ok(resumeMatchService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get resume match by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Match found"),
			@ApiResponse(responseCode = "404", description = "Match not found")})
	public ResponseEntity<ResumeMatch> getById(@Parameter(description = "Match ID") @PathVariable("id") Long id) {
		return ResponseEntity.ok(resumeMatchService.findById(id));
	}

	@GetMapping("/resume/{resumeId}")
	@Operation(summary = "Get all matches for a resume, sorted by match percent descending")
	public ResponseEntity<List<ResumeMatch>> getByResumeId(
			@Parameter(description = "Resume ID") @PathVariable("resumeId") Long resumeId) {
		return ResponseEntity.ok(resumeMatchService.findByResumeId(resumeId));
	}

	@GetMapping("/assignment/{assignmentId}")
	@Operation(summary = "Get all matches for an assignment")
	public ResponseEntity<List<ResumeMatch>> getByAssignmentId(
			@Parameter(description = "Assignment ID") @PathVariable("assignmentId") Long assignmentId) {
		return ResponseEntity.ok(resumeMatchService.findByAssignmentId(assignmentId));
	}

	@PostMapping
	@Operation(summary = "Create a resume match record")
	@ApiResponses({@ApiResponse(responseCode = "201", description = "Match created"),
			@ApiResponse(responseCode = "409", description = "Match already exists for this resume/assignment pair")})
	public ResponseEntity<ResumeMatch> create(@RequestBody ResumeMatch resumeMatch) {
		return ResponseEntity.status(HttpStatus.CREATED).body(resumeMatchService.save(resumeMatch));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a resume match record")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Match updated"),
			@ApiResponse(responseCode = "404", description = "Match not found")})
	public ResponseEntity<ResumeMatch> update(@Parameter(description = "Match ID") @PathVariable("id") Long id,
			@RequestBody ResumeMatch resumeMatch) {
		return ResponseEntity.ok(resumeMatchService.update(id, resumeMatch));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a resume match record")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Match deleted"),
			@ApiResponse(responseCode = "404", description = "Match not found")})
	public ResponseEntity<Void> delete(@Parameter(description = "Match ID") @PathVariable("id") Long id) {
		resumeMatchService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
