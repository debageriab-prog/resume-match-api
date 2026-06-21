package se.debageri.api.entity;

import java.time.Instant;
import java.time.LocalDate;

public interface HighMatchRow {
	long getAssignmentId();
	LocalDate getPublishedOn();
	Instant getApplicationDeadline();
	String getTitle();
	String getUrl();
	String getPortal();
	int getMatchPercent();
	String getReasonsJson();
	String getMissingMustHavesJson();
	String getDecision();
}
