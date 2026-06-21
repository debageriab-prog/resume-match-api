package se.debageri.api.dto;

import se.debageri.api.entity.NotificationType;

public record ResumeUpdateRequest(String managerEmail, NotificationType notificationType) {
}
