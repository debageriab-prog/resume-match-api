package se.debageri.api.controller;

import java.util.List;

import jakarta.validation.constraints.Email;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import se.debageri.api.dto.ResumeSummaryDto;
import se.debageri.api.dto.ResumeTopMatchedDto;
import se.debageri.api.dto.ResumeUpdateDto;
import se.debageri.api.dto.ResumeUpdateRequest;
import se.debageri.api.dto.StatisticsResponse;
import se.debageri.api.entity.NotificationType;
import se.debageri.api.entity.Resume;
import se.debageri.api.service.ResumeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes", description = "Manage candidate resumes")
public class ResumeController {

	private final ResumeService resumeService;

	@GetMapping("/statistics")
	@Operation(summary = "Get resume statistics (total, today, last week, last month)")
	public ResponseEntity<StatisticsResponse> getStatistics() {
		return ResponseEntity.ok(resumeService.getStatistics());
	}

	@GetMapping("/topmatched")
	@Operation(summary = "Get top 5 most recently uploaded resumes that have at least one positive match (decision: maybe, yes, strong_yes), with file name, owner name, upload time and match count")
	public ResponseEntity<List<ResumeTopMatchedDto>> getTopMatched() {
		return ResponseEntity.ok(resumeService.getTopMatched());
	}

	@GetMapping
	@Operation(summary = "Get all resumes (summary view) with pagination")
	public ResponseEntity<Page<ResumeSummaryDto>> getAll(
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(resumeService.findAll(pageable).map(this::toSummary));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get resume summary by ID")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resume found"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<ResumeSummaryDto> getById(@Parameter(description = "Resume ID") @PathVariable("id") Long id) {
		return ResponseEntity.ok(toSummary(resumeService.findById(id)));
	}

	@GetMapping("/owner/{ownerId}")
	@Operation(summary = "Get all resumes (summary view) for a seeker with pagination")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resumes retrieved"),
			@ApiResponse(responseCode = "404", description = "Seeker not found")})
	public ResponseEntity<Page<ResumeSummaryDto>> getByOwnerId(
			@Parameter(description = "Seeker ID") @PathVariable("ownerId") Long ownerId,
			@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
		return ResponseEntity.ok(resumeService.findByOwnerId(ownerId, pageable).map(this::toSummary));
	}

	@Operation(summary = "Upload a resume PDF and save it (extracts owner identity automatically via LLM)")
	@ApiResponse(responseCode = "200", description = "Saved resume id")
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public SavedResumeResponse uploadResume(
			@Parameter(name = "resume", description = "Resume PDF file", required = true, content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))) @RequestPart("resume") MultipartFile resumePdf,
			@Parameter(name = "managerEmail", description = "Optional manager email address for notifications") @Email @RequestParam(name = "managerEmail", required = false) String managerEmail,
			@Parameter(name = "notificationType", description = "Who should receive notifications: User, Manager, or Both", required = true) @RequestParam("notificationType") NotificationType notificationType) {
		long resumeId = resumeService.saveResumeFromPdf(resumePdf, managerEmail, notificationType);
		return new SavedResumeResponse(resumeId);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a resume (managerEmail and notificationType only)")
	@ApiResponses({@ApiResponse(responseCode = "200", description = "Resume updated"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	public ResponseEntity<ResumeUpdateDto> update(@Parameter(description = "Resume ID") @PathVariable("id") Long id,
			@RequestBody ResumeUpdateRequest request) {
		return ResponseEntity.ok(toUpdateDto(resumeService.update(id, request)));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a resume, its match records, and the owner if they have no other resumes")
	@ApiResponses({@ApiResponse(responseCode = "204", description = "Resume deleted"),
			@ApiResponse(responseCode = "404", description = "Resume not found")})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@Parameter(description = "Resume ID") @PathVariable("id") Long id) {
		resumeService.delete(id);
	}

	private ResumeSummaryDto toSummary(Resume resume) {
		long matchedCount = resumeService.getMatchedCount(resume.getId());
		return new ResumeSummaryDto(resume.getId(), resume.getOwner(), resume.getManagerEmail(),
				resume.getNotificationType(), resume.getFileName(), resume.getCreatedAt(), matchedCount);
	}

	private ResumeUpdateDto toUpdateDto(Resume resume) {
		return new ResumeUpdateDto(resume.getId(), resume.getOwner(), resume.getManagerEmail(),
				resume.getNotificationType());
	}

	public record SavedResumeResponse(long resumeId) {
	}
}
