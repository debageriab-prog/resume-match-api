package se.debageri.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResumeMatchRunResponse {
	private long resumeId;
	private int newMatches;
}
