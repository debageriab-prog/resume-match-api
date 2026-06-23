package se.debageri.api.dto;

import java.time.Instant;

public record ResumeTopMatchedDto(
		Long id,
		String fileName,
		String ownerName,
		Instant createdAt,
		long matchCount) {
}
