package se.debageri.api.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeProfileDTO {
	private String name;
	private String title;
	private String summary;
	private Integer yearsOfExperience;
	private List<String> roles;
	private List<String> skills;
	private List<String> tools;
	private List<String> domains;
	private List<Map<String, String>> languages; // [{name:"Swedish", level:"C1"}]
	private List<String> locations;
	private String remotePreference; // remote|hybrid|onsite|any
	private String availabilityDate; // YYYY-MM-DD
	private List<String> education;
	private List<String> certifications;
}
