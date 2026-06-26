package se.debageri.api.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.dto.ResumeMatchDto;
import se.debageri.api.dto.ResumeMatchTopMatchedDto;
import se.debageri.api.dto.StatisticsResponse;
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
	@Operation(summary = "Get all resume matches with optional filtering and pagination. "
			+ "By default only positive decisions (not null, not 'no') are returned; "
			+ "pass includeNegativeDecisions=true to include null and 'no' decisions as well.")
	public ResponseEntity<Page<ResumeMatchDto>> getAll(
			@Parameter(description = "Filter by exact assignment ID") @RequestParam(required = false) Long assignmentId,
			@Parameter(description = "Filter by exact resume ID") @RequestParam(required = false) Long resumeId,
			@Parameter(description = "Filter by assignment title (partial, case-insensitive)") @RequestParam(required = false) String assignmentTitle,
			@Parameter(description = "Filter by resume file name (partial, case-insensitive)") @RequestParam(required = false) String resumeFileName,
			@Parameter(description = "Filter by owner full name (partial, case-insensitive)") @RequestParam(required = false) String ownerName,
			@Parameter(description = "When true, include null and 'no' decisions. Default false: only positive decisions (maybe, yes, strong_yes) are returned") @RequestParam(required = false) Boolean includeNegativeDecisions,
			@Parameter(description = "Filter by exact decision value (no, maybe, yes, strong_yes)") @RequestParam(required = false) String decision,
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(resumeMatchService.findAll(assignmentId, resumeId, assignmentTitle, resumeFileName,
				ownerName, includeNegativeDecisions, decision, pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get resume match by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Match found"),
			@ApiResponse(responseCode = "404", description = "Match not found")})
	public ResponseEntity<ResumeMatchDto> getById(@Parameter(description = "Match ID") @PathVariable("id") Long id) {
		return ResponseEntity.ok(resumeMatchService.findById(id));
	}

	@GetMapping("/resume/{resumeId}")
	@Operation(summary = "Get all matches for a resume with pagination, sorted by match percent descending")
	public ResponseEntity<Page<ResumeMatchDto>> getByResumeId(
			@Parameter(description = "Resume ID") @PathVariable("resumeId") Long resumeId,
			@ParameterObject @PageableDefault(size = 10, sort = "matchPercent", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(resumeMatchService.findByResumeId(resumeId, pageable));
	}

	@GetMapping("/assignment/{assignmentId}")
	@Operation(summary = "Get all matches for an assignment")
	public ResponseEntity<Page<ResumeMatchDto>> getByAssignmentId(
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
