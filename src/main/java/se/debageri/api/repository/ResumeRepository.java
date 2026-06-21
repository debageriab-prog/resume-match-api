package se.debageri.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import se.debageri.api.entity.Resume;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long>, JpaSpecificationExecutor<Resume> {

	List<Resume> findByOwnerId(Long ownerId);

	Page<Resume> findAll(Pageable pageable);

	long countByOwnerId(Long ownerId);
}
