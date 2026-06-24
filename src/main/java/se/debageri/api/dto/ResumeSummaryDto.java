package se.debageri.api.dto;

import java.time.Instant;

import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.NotificationType;

public record ResumeSummaryDto(Long id, AssignmentSeeker owner, String managerEmail,
		NotificationType notificationType, String fileName, Instant createdAt, long matchedCount) {
}
