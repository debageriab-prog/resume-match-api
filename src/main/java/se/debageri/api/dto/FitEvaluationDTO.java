package se.debageri.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FitEvaluationDTO {
	private Integer fit; // 0..100
	private String decision; // "strong_yes"|"yes"|"maybe"|"no"
	@JsonProperty("missing_must_haves")
	private List<String> missingMustHaves;
	private List<String> reasons;
}
