package se.debageri.api.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.dto.StatisticsResponse;
import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.service.AssignmentSeekerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assignment-seekers")
@RequiredArgsConstructor
@Tag(name = "Assignment Seekers", description = "Manage job seekers")
public class AssignmentSeekerController {

	private final AssignmentSeekerService assignmentSeekerService;

	@GetMapping("/statistics")
	@Operation(summary = "Get assignment seeker statistics (total, today, last week, last month)")
	public ResponseEntity<StatisticsResponse> getStatistics() {
		return ResponseEntity.ok(assignmentSeekerService.getStatistics());
	}

	@GetMapping
	@Operation(summary = "Get all assignment seekers with pagination")
	public ResponseEntity<Page<AssignmentSeeker>> getAll(
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(assignmentSeekerService.findAll(pageable));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get assignment seeker by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Seeker found"),
			@ApiResponse(responseCode = "404", description = "Seeker not found")})
	public ResponseEntity<AssignmentSeeker> getById(@Parameter(description = "Seeker ID") @PathVariable("id") Long id) {
		return ResponseEntity.ok(assignmentSeekerService.findById(id));
	}

	@PostMapping
	@Operation(summary = "Create a new assignment seeker")
	@ApiResponses({@ApiResponse(responseCode = "201", description = "Seeker created"),
			@ApiResponse(responseCode = "409", description = "Email already exists")})
	public ResponseEntity<AssignmentSeeker> create(@RequestBody AssignmentSeeker seeker) {
		return ResponseEntity.status(HttpStatus.CREATED).body(assignmentSeekerService.save(seeker));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an assignment seeker")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Seeker updated"),
			@ApiResponse(responseCode = "404", description = "Seeker not found"),
			@ApiResponse(responseCode = "409", description = "Email already in use")})
	public ResponseEntity<AssignmentSeeker> update(@Parameter(description = "Seeker ID") @PathVariable("id") Long id,
			@RequestBody AssignmentSeeker seeker) {
		return ResponseEntity.ok(assignmentSeekerService.update(id, seeker));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete an assignment seeker")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Seeker deleted"),
			@ApiResponse(responseCode = "404", description = "Seeker not found")})
	public ResponseEntity<Void> delete(@Parameter(description = "Seeker ID") @PathVariable("id") Long id) {
		assignmentSeekerService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
