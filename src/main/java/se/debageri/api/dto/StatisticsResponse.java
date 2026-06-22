package se.debageri.api.dto;

public record StatisticsResponse(long totalCount, long todayCount, long lastWeekCount, long lastMonthCount) {
}
