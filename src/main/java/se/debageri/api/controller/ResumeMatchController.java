package se.debageri.api.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.dto.ResumeMatchTopMatchedDto;
import se.debageri.api.dto.StatisticsResponse;
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

	@GetMapping("/statistics")
	@Operation(summary = "Get resume match statistics (total, today, last week, last month)")
	public ResponseEntity<StatisticsResponse> getStatistics() {
		return ResponseEntity.ok(resumeMatchService.getStatistics());
	}

	@GetMapping("/topmatched")
	@Operation(summary = "Get top 5 latest matches with a positive decision (maybe, yes, strong_yes), showing seeker, resume file name, match percent and judged at")
	public ResponseEntity<List<ResumeMatchTopMatchedDto>> getTopMatched() {
		return ResponseEntity.ok(resumeMatchService.getTopMatched());
	}

	@GetMapping
	@Operation(summary = "Get all resume matches with pagination")
	public ResponseEntity<Page<ResumeMatch>> getAll(
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(resumeMatchService.findAll(pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get resume match by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Match found"),
			@ApiResponse(responseCode = "404", description = "Match not found")})
	public ResponseEntity<ResumeMatch> getById(@Parameter(description = "Match ID") @PathVariable("id") Long id) {
		return ResponseEntity.ok(resumeMatchService.findById(id));
	}

	@GetMapping("/resume/{resumeId}")
	@Operation(summary = "Get all matches for a resume with pagination, sorted by match percent descending")
	public ResponseEntity<Page<ResumeMatch>> getByResumeId(
			@Parameter(description = "Resume ID") @PathVariable("resumeId") Long resumeId,
			@ParameterObject @PageableDefault(size = 10, sort = "matchPercent", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(resumeMatchService.findByResumeId(resumeId, pageable));
	}

	@GetMapping("/assignment/{assignmentId}")
	@Operation(summary = "Get all matches for an assignment")
	public ResponseEntity<Page<ResumeMatch>> getByAssignmentId(
			@Parameter(description = "Assignment ID") @PathVariable("assignmentId") Long assignmentId,
			@ParameterObject @PageableDefault(size = 10, sort = "matchPercent", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(resumeMatchService.findByAssignmentId(assignmentId, pageable));
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
