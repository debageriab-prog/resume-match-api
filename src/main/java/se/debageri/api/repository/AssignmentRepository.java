package se.debageri.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

	Optional<Assignment> findByJobId(Long jobId);

	boolean existsByJobId(Long jobId);
}
