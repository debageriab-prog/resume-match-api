package se.debageri.api.repository;

import org.springframework.data.jpa.domain.Specification;

import se.debageri.api.entity.Assignment;

public class AssignmentSpecification {

	private AssignmentSpecification() {
	}

	public static Specification<Assignment> hasJobId(Long jobId) {
		return (root, query, cb) -> jobId == null ? cb.conjunction() : cb.equal(root.get("jobId"), jobId);
	}

	public static Specification<Assignment> titleContains(String title) {
		return (root, query, cb) -> title == null || title.isBlank()
				? cb.conjunction()
				: cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
	}

	public static Specification<Assignment> clientContains(String client) {
		return (root, query, cb) -> client == null || client.isBlank()
				? cb.conjunction()
				: cb.like(cb.lower(root.get("client")), "%" + client.toLowerCase() + "%");
	}

	public static Specification<Assignment> locationContains(String location) {
		return (root, query, cb) -> location == null || location.isBlank()
				? cb.conjunction()
				: cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
	}

	public static Specification<Assignment> hasPortal(String portal) {
		return (root, query, cb) -> portal == null || portal.isBlank()
				? cb.conjunction()
				: cb.equal(cb.lower(root.get("portal")), portal.toLowerCase());
	}
}
