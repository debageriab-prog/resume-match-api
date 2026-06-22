package se.debageri.api.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assignment")
@Getter
@Setter
public class Assignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "job_id", nullable = false, unique = true)
	private Long jobId;

	@Column(length = 500)
	private String title;

	@Column(length = 255)
	private String client;

	@Column(name = "published_on")
	private LocalDate publishedOn;

	@Column(name = "application_deadline")
	private LocalDateTime applicationDeadline;

	@Column(length = 255)
	private String role;

	@Column(name = "seniority_level", length = 255)
	private String seniorityLevel;

	@Column(length = 500)
	private String location;

	@Column(length = 100)
	private String remote;

	@Column(name = "period_start")
	private LocalDate periodStart;

	@Column(name = "period_end")
	private LocalDate periodEnd;

	@Lob
	private String description;

	@Lob
	@Column(name = "required_skills")
	private String requiredSkills;

	@Lob
	@Column(name = "preferred_skills")
	private String preferredSkills;

	@Lob
	private String languages;

	@Column(length = 1024)
	private String url;

	private String portal;
}
