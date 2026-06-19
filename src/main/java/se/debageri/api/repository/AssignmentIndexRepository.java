package se.debageri.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.AssignmentIndex;

@Repository
public interface AssignmentIndexRepository extends JpaRepository<AssignmentIndex, Long> {
}
