package se.debageri.api.entity;

import java.time.Instant;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resume")
@Getter
@Setter
public class Resume {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "owner_id", nullable = false)
	private AssignmentSeeker owner;

	@Column(name = "file_name", length = 255)
	private String fileName;

	@Column(name = "content_type", length = 100)
	private String contentType;

	@Lob
	@Column(name = "pdf_bytes")
	private byte[] pdfBytes;

	@Lob
	@Column(name = "extracted_text")
	private String extractedText;

	@Lob
	@Column(name = "profile_json", columnDefinition = "json")
	private String profileJson;

	@Column(name = "manager_email", length = 255)
	private String managerEmail;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 20)
	private NotificationType notificationType;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();
}
