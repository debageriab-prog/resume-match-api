package se.debageri.api.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.AssignmentSeeker;

@Repository
public interface AssignmentSeekerRepository extends JpaRepository<AssignmentSeeker, Long> {

	Optional<AssignmentSeeker> findByEmail(String email);

	boolean existsByEmail(String email);

	long countByCreatedAtBetween(Instant start, Instant end);
}
