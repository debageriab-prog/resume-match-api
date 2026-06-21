package se.debageri.api.entity;

import java.time.Instant;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assignment_seeker")
@Getter
@Setter
public class AssignmentSeeker {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(name = "email", nullable = false, unique = true, length = 255)
	private String email;

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();
}
