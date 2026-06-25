package se.debageri.api.dto;

import java.time.Instant;

public record ResumeMatchDto(
		Long id,
		AssignmentSummary assignment,
		ResumeSummary resume,
		double score,
		int matchPercent,
		Instant matchedAt,
		String reasonsJson,
		String missingMustHavesJson,
		String decision,
		Instant judgedAt,
		String judgeModel
) {

	public record AssignmentSummary(Long id, String title) {
	}

	public record ResumeSummary(Long id, String fileName, String ownerFullName) {
	}
}
