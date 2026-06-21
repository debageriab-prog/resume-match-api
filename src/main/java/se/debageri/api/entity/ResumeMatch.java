package se.debageri.api.entity;

import java.time.Instant;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "resume_match", uniqueConstraints = @UniqueConstraint(name = "uq_resume_job", columnNames = {"resume_id",
		"assignment_id"}))
@Getter
@Setter
public class ResumeMatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "resume_id", nullable = false)
	private Long resumeId;

	@Column(name = "assignment_id", nullable = false)
	private Long assignmentId;

	@Column(name = "score", nullable = false)
	private double score;

	@Column(name = "match_percent", nullable = false)
	private int matchPercent;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Column(name = "matched_at", nullable = false, updatable = false)
	private final Instant matchedAt = Instant.now();

	@Column(name = "reasons", columnDefinition = "json")
	private String reasonsJson;

	@Column(name = "missing_must_haves", columnDefinition = "json")
	private String missingMustHavesJson;

	@Column(name = "decision")
	private String decision;

	@Column(name = "judged_at")
	private Instant judgedAt;

	@Column(name = "judge_model", length = 100)
	private String judgeModel;
}
