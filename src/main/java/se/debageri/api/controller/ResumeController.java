package se.debageri.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import se.debageri.api.dto.ResumeSummaryDto;
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
	@Operation(summary = "Get all resumes (summary view) with pagination")
	public ResponseEntity<Page<ResumeSummaryDto>> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(resumeService.findAll(pageable).map(this::toSummary));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get resume summary by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resume found"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<ResumeSummaryDto> getById(@Parameter(description = "Resume ID") @PathVariable Long id) {
		return ResponseEntity.ok(toSummary(resumeService.findById(id)));
	}

	@GetMapping("/owner/{ownerId}")
	@Operation(summary = "Get all resumes (summary view) for a seeker with pagination")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resumes retrieved"),
			@ApiResponse(responseCode = "404", description = "Seeker not found")})
	public ResponseEntity<Page<ResumeSummaryDto>> getByOwnerId(
			@Parameter(description = "Seeker ID") @PathVariable Long ownerId,
			@PageableDefault(size = 20, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(resumeService.findByOwnerId(ownerId, pageable).map(this::toSummary));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a resume (fileName, contentType, pdfBytes, extractedText, profileJson, managerEmail, notificationType)")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resume updated"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<ResumeSummaryDto> update(@Parameter(description = "Resume ID") @PathVariable Long id,
			@RequestBody Resume resume) {
		return ResponseEntity.ok(toSummary(resumeService.update(id, resume)));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a resume, its match records, and the owner if they have no other resumes")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Resume deleted"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<Void> delete(@Parameter(description = "Resume ID") @PathVariable Long id) {
		resumeService.delete(id);
		return ResponseEntity.noContent().build();
	}

	private ResumeSummaryDto toSummary(Resume resume) {
		return new ResumeSummaryDto(resume.getId(), resume.getOwner(), resume.getManagerEmail(),
				resume.getNotificationType());
	}
}
