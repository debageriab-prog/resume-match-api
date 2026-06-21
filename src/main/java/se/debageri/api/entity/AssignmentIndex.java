package se.debageri.api.entity;

import java.time.Instant;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assignment_index")
@Getter
@Setter
public class AssignmentIndex {

	@Id
	@Column(name = "assignment_id", nullable = false)
	private Long assignmentId;

	@Column(name = "indexed_at", nullable = false)
	private Instant indexedAt = Instant.now();
}
