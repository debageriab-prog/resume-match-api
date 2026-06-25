package se.debageri.api.dto;

import se.debageri.api.entity.AssignmentSeeker;
import se.debageri.api.entity.NotificationType;

public record ResumeUpdateDto(Long id, AssignmentSeeker owner, String managerEmail, NotificationType notificationType) {
}
