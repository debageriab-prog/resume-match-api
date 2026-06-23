package se.debageri.api.dto;

import java.time.LocalDate;

public record AssignmentTopMatchedDto(Long id, String title, String client, LocalDate publishedOn, long matchCount) {
}
