package se.debageri.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.entity.Resume;
import se.debageri.api.service.ResumeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes", description = "Manage candidate resumes")
public class ResumeController {

	private final ResumeService resumeService;

	@GetMapping
	@Operation(summary = "Get all resumes")
	public ResponseEntity<List<Resume>> getAll() {
		return ResponseEntity.ok(resumeService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get resume by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resume found"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<Resume> getById(@Parameter(description = "Resume ID") @PathVariable Long id) {
		return ResponseEntity.ok(resumeService.findById(id));
	}

	@GetMapping("/owner/{ownerId}")
	@Operation(summary = "Get all resumes for a seeker")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resumes retrieved"),
			@ApiResponse(responseCode = "404", description = "Seeker not found")})
	public ResponseEntity<List<Resume>> getByOwnerId(@Parameter(description = "Seeker ID") @PathVariable Long ownerId) {
		return ResponseEntity.ok(resumeService.findByOwnerId(ownerId));
	}

	@PostMapping("/owner/{ownerId}")
	@Operation(summary = "Create a resume for a seeker")
	@ApiResponses({@ApiResponse(responseCode = "201", description = "Resume created"),
			@ApiResponse(responseCode = "404", description = "Seeker not found")})
	public ResponseEntity<Resume> create(@Parameter(description = "Seeker ID") @PathVariable Long ownerId,
			@RequestBody Resume resume) {
		return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.save(ownerId, resume));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a resume")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resume updated"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<Resume> update(@Parameter(description = "Resume ID") @PathVariable Long id,
			@RequestBody Resume resume) {
		return ResponseEntity.ok(resumeService.update(id, resume));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a resume")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Resume deleted"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<Void> delete(@Parameter(description = "Resume ID") @PathVariable Long id) {
		resumeService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
