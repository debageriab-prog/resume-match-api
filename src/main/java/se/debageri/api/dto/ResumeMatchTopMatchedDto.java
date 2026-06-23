package se.debageri.api.dto;

import java.time.Instant;

import se.debageri.api.entity.AssignmentSeeker;

public record ResumeMatchTopMatchedDto(
		AssignmentSeeker assignmentSeeker,
		String resumeFileName,
		int matchPercent,
		Instant judgedAt) {
}
