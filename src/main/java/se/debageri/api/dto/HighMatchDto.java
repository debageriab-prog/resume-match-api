package se.debageri.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HighMatchDto {
	private long assignmentId;
	private LocalDate publishedOn;
	private Instant applicationDeadline;
	private String title;
	private String url;
	private String portal;
	private FitEvaluationDTO fitEvaluation;
}
