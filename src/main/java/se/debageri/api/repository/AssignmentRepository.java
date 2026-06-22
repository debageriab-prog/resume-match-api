package se.debageri.api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>, JpaSpecificationExecutor<Assignment> {

	Optional<Assignment> findByJobId(Long jobId);

	boolean existsByJobId(Long jobId);

	void deleteByIdIn(List<Long> ids);

	long countByPublishedOnBetween(LocalDate start, LocalDate end);
}
