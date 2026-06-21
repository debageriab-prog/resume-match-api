package se.debageri.api.service;

import static se.debageri.api.util.StringUtil.isBlank;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.debageri.api.dto.AssignmentSeekerInfoDTO;
import se.debageri.api.dto.ResumeProfileDTO;
import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.NotificationType;
import se.debageri.api.entity.Resume;
import se.debageri.api.exception.ResumeNotFoundException;
import se.debageri.api.repository.AssignmentSeekerRepository;
import se.debageri.api.repository.ResumeMatchRepository;
import se.debageri.api.repository.ResumeRepository;
import se.debageri.api.util.EmailExtractor;
import se.debageri.api.util.StringUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeService {

	private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

	private final ResumeRepository resumeRepository;
	private final ResumeMatchRepository resumeMatchRepository;
	private final AssignmentSeekerRepository assignmentSeekerRepository;
	private final AssignmentSeekerService assignmentSeekerService;
	private final OpenAiService openAiService;
	private final ObjectMapper objectMapper;

	public Page<Resume> findAll(Pageable pageable) {
		return resumeRepository.findAll(pageable);
	}

	public Resume findById(Long id) {
		return resumeRepository.findById(id).orElseThrow(() -> new ResumeNotFoundException(id));
	}

	public Page<Resume> findByOwnerId(Long ownerId, Pageable pageable) {
		assignmentSeekerService.findById(ownerId);
		return resumeRepository.findAll((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId), pageable);
	}

	@Transactional
	public Resume update(Long id, Resume updated) {
		Resume existing = findById(id);
		existing.setManagerEmail(updated.getManagerEmail());
		existing.setNotificationType(updated.getNotificationType());
		return resumeRepository.save(existing);
	}

	/**
	 * Saves a resume PDF to MySQL, extracting assignmentSeeker identity (first
	 * name, last name, email).
	 *
	 * <p>
	 * Flow:
	 * <ul>
	 * <li>Extract text from PDF</li>
	 * <li>Extract email/name via LLM (plus optional regex fallback)</li>
	 * <li>Upsert assignmentSeeker by email</li>
	 * <li>Store PDF bytes, extracted text, and profile JSON</li>
	 * </ul>
	 *
	 * @param pdf
	 *            resume PDF uploaded by user
	 * @param managerEmail
	 *            optional manager email for notifications (may be null)
	 * @param notificationType
	 *            who should receive notifications
	 * @return saved resume id
	 */
	@Transactional
	public long saveResumeFromPdf(MultipartFile pdf, String managerEmail, NotificationType notificationType) {
		if (notificationType != NotificationType.User && isBlank(managerEmail)) {
			throw new IllegalArgumentException("managerEmail is required when notificationType is " + notificationType);
		}

		String extractedText = StringUtil.extractTextFromPDF(pdf);

		// 1) Extract assignmentSeeker identity
		AssignmentSeekerInfoDTO assignmentSeekerInfoDTO = openAiService.extractAssignmentSeekerInfo(extractedText);

		// 2) Fallback: if LLM missed email, try regex
		if (assignmentSeekerInfoDTO == null) {
			assignmentSeekerInfoDTO = new AssignmentSeekerInfoDTO();
		}
		if (isBlank(assignmentSeekerInfoDTO.getEmail())) {
			assignmentSeekerInfoDTO.setEmail(EmailExtractor.findFirstEmail(extractedText).orElse(null));
		}

		if (isBlank(assignmentSeekerInfoDTO.getEmail())) {
			throw new IllegalArgumentException(
					"Could not find an email in the resume. Please ensure the resume contains an email address.");
		}

		// 3) Upsert assignmentSeeker by email
		AssignmentSeeker assignmentSeeker = upsertAssignmentSeeker(assignmentSeekerInfoDTO);

		// 4) Build structured resume profile
		ResumeProfileDTO profile = openAiService.extractStructuredProfile(extractedText);
		String profileJson = writeJson(profile);

		// 5) Save resume
		Resume resume = new Resume();
		resume.setOwner(assignmentSeeker);
		resume.setFileName(pdf.getOriginalFilename());
		resume.setContentType(pdf.getContentType());
		resume.setExtractedText(extractedText);
		resume.setProfileJson(profileJson);
		resume.setManagerEmail(managerEmail);
		resume.setNotificationType(notificationType);

		try {
			resume.setPdfBytes(pdf.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("Failed reading PDF bytes: " + e.getMessage(), e);
		}

		Resume saved = resumeRepository.save(resume);
		log.info("Saved resume id={} for assignmentSeeker email={}", saved.getId(), assignmentSeeker.getEmail());
		return saved.getId();
	}

	/**
	 * Finds assignmentSeeker by email, or creates a new one. If the seeker exists
	 * and name fields are blank, updates them.
	 */
	private AssignmentSeeker upsertAssignmentSeeker(AssignmentSeekerInfoDTO dto) {
		String email = dto.getEmail().trim().toLowerCase();

		Optional<AssignmentSeeker> existing = assignmentSeekerRepository.findByEmail(email);
		if (existing.isPresent()) {
			AssignmentSeeker old = existing.get();
			boolean changed = false;

			if (isBlank(old.getFirstName()) && !isBlank(dto.getFirstName())) {
				old.setFirstName(dto.getFirstName().trim());
				changed = true;
			}
			if (isBlank(old.getLastName()) && !isBlank(dto.getLastName())) {
				old.setLastName(dto.getLastName().trim());
				changed = true;
			}

			if (changed) {
				assignmentSeekerRepository.save(old);
				log.info("Updated assignmentSeeker names for email={}", email);
			}
			return old;
		}

		AssignmentSeeker seeker = new AssignmentSeeker();
		seeker.setEmail(email);
		seeker.setFirstName(!isBlank(dto.getFirstName()) ? dto.getFirstName().trim() : "Unknown");
		seeker.setLastName(!isBlank(dto.getLastName()) ? dto.getLastName().trim() : "Unknown");
		AssignmentSeeker saved = assignmentSeekerRepository.save(seeker);

		log.info("Created assignmentSeeker id={} email={}", saved.getId(), saved.getEmail());
		return saved;
	}

	private String writeJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize JSON: " + e.getMessage(), e);
		}
	}

	@Transactional
	public void delete(long resumeId) {
		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new ResumeNotFoundException(resumeId));

		Long ownerId = resume.getOwner() != null ? resume.getOwner().getId() : null;

		resumeMatchRepository.deleteByResumeId(resumeId);

		resumeRepository.deleteById(resumeId);

		if (ownerId != null) {
			long remaining = resumeRepository.countByOwnerId(ownerId);
			if (remaining == 0) {
				assignmentSeekerRepository.deleteById(ownerId);
				log.info("Deleted owner assignmentSeeker id={}", ownerId);
			}
		}

		log.info("Deleted resume id={}", resumeId);
	}
}
