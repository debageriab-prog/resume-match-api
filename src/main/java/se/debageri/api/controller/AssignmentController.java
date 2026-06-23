package se.debageri.api.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.dto.StatisticsResponse;
import se.debageri.api.entity.Assignment;
import se.debageri.api.service.AssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Manage job assignments")
public class AssignmentController {

	private final AssignmentService assignmentService;

	@GetMapping("/statistics")
	@Operation(summary = "Get assignment statistics (total, today, last week, last month)")
	public ResponseEntity<StatisticsResponse> getStatistics() {
		return ResponseEntity.ok(assignmentService.getStatistics());
	}

	@GetMapping
	@Operation(summary = "Get all assignments with optional filtering and pagination")
	public ResponseEntity<Page<Assignment>> getAll(
			@Parameter(description = "Filter by exact jobId") @RequestParam(required = false) Long jobId,
			@Parameter(description = "Filter by title (partial match)") @RequestParam(required = false) String title,
			@Parameter(description = "Filter by client (partial match)") @RequestParam(required = false) String client,
			@Parameter(description = "Filter by location (partial match)") @RequestParam(required = false) String location,
			@Parameter(description = "Filter by portal (exact match)") @RequestParam(required = false) String portal,
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(assignmentService.findAll(jobId, title, client, location, portal, pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get assignment by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Assignment found"),
			@ApiResponse(responseCode = "404", description = "Assignment not found")})
	public ResponseEntity<Assignment> getById(@Parameter(description = "Assignment ID") @PathVariable("id") Long id) {
		return ResponseEntity.ok(assignmentService.findById(id));
	}

	@PostMapping
	@Operation(summary = "Create a new assignment")
	@ApiResponses({@ApiResponse(responseCode = "201", description = "Assignment created"),
			@ApiResponse(responseCode = "409", description = "Duplicate jobId")})
	public ResponseEntity<Assignment> create(@RequestBody Assignment assignment) {
		return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.save(assignment));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an assignment")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Assignment updated"),
			@ApiResponse(responseCode = "404", description = "Assignment not found")})
	public ResponseEntity<Assignment> update(@Parameter(description = "Assignment ID") @PathVariable("id") Long id,
			@RequestBody Assignment assignment) {
		return ResponseEntity.ok(assignmentService.update(id, assignment));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete an assignment and its related match records")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Assignment deleted"),
			@ApiResponse(responseCode = "404", description = "Assignment not found")})
	public ResponseEntity<Void> delete(@Parameter(description = "Assignment ID") @PathVariable("id") Long id) {
		assignmentService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
