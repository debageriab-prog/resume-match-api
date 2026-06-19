package se.debageri.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.Resume;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

	List<Resume> findByOwnerId(Long ownerId);
}
