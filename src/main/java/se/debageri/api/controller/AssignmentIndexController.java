package se.debageri.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.entity.AssignmentIndex;
import se.debageri.api.service.AssignmentIndexService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assignment-indexes")
@RequiredArgsConstructor
@Tag(name = "Assignment Indexes", description = "Track which assignments have been indexed")
public class AssignmentIndexController {

	private final AssignmentIndexService assignmentIndexService;

	@GetMapping
	@Operation(summary = "Get all assignment index entries")
	public ResponseEntity<List<AssignmentIndex>> getAll() {
		return ResponseEntity.ok(assignmentIndexService.findAll());
	}

	@GetMapping("/{assignmentId}")
	@Operation(summary = "Get assignment index by assignment ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Entry found"),
			@ApiResponse(responseCode = "404", description = "Entry not found")})
	public ResponseEntity<AssignmentIndex> getById(
			@Parameter(description = "Assignment ID") @PathVariable Long assignmentId) {
		return ResponseEntity.ok(assignmentIndexService.findById(assignmentId));
	}

	@PostMapping
	@Operation(summary = "Create an assignment index entry")
	@ApiResponse(responseCode = "201", description = "Entry created")
	public ResponseEntity<AssignmentIndex> create(@RequestBody AssignmentIndex assignmentIndex) {
		return ResponseEntity.status(HttpStatus.CREATED).body(assignmentIndexService.save(assignmentIndex));
	}

	@DeleteMapping("/{assignmentId}")
	@Operation(summary = "Delete an assignment index entry")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Entry deleted"),
			@ApiResponse(responseCode = "404", description = "Entry not found")})
	public ResponseEntity<Void> delete(@Parameter(description = "Assignment ID") @PathVariable Long assignmentId) {
		assignmentIndexService.delete(assignmentId);
		return ResponseEntity.noContent().build();
	}
}
