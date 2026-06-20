package se.debageri.api.service;

import static se.debageri.api.util.StringUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import se.debageri.api.dto.AssignmentSeekerInfoDTO;
import se.debageri.api.dto.ResumeProfileDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenAiService {

	private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

	private final OpenAIClient client;
	private final ObjectMapper mapper;

	public static final String PROFILE_MODEL = "gpt-4o";

	/**
	 * Extracts the candidate's identity (first name, last name, email) from raw
	 * resume text using the LLM.
	 *
	 * @param resumeText
	 *            the full plain-text content of the resume
	 * @return a DTO with the extracted identity fields; individual fields may be
	 *         {@code null} if the model cannot determine them with confidence
	 * @throws RuntimeException
	 *             if the model response cannot be parsed as JSON
	 */
	public AssignmentSeekerInfoDTO extractAssignmentSeekerInfo(String resumeText) {
		String schema = """
				Return ONLY valid JSON. No markdown.
				Schema:
				{
				  "firstName": string|null,
				  "lastName": string|null,
				  "email": string|null
				}
				Rules:
				- Use the candidate's full name (not employer).
				- Prefer the email shown in the resume.
				- If unsure, use null.
				""";

		String input = """
				Extract the candidate first name, last name and email from this resume text.

				%s

				RESUME TEXT:
				%s
				""".formatted(schema, safeLimit(resumeText, 12000));

		ResponseCreateParams params = ResponseCreateParams.builder().model(PROFILE_MODEL).input(input).build();

		Response resp = client.responses().create(params);
		String json = stripJsonCodeFences(extractText(resp));

		try {
			return mapper.readValue(json, AssignmentSeekerInfoDTO.class);
		} catch (Exception e) {
			throw new RuntimeException("Owner extraction JSON was not parseable. Raw:\n" + json, e);
		}
	}

	/**
	 * Extracts a structured candidate profile from raw resume text using the LLM.
	 *
	 * @param resumeText
	 *            the full plain-text content of the resume
	 * @return a {@link ResumeProfileDTO} populated with normalized structured data
	 * @throws RuntimeException
	 *             if the model response cannot be parsed as JSON
	 */
	public ResumeProfileDTO extractStructuredProfile(String resumeText) {
		String schemaHint = """
				Return ONLY valid JSON. No markdown. No extra text.
				JSON schema:
				{
				  "name": string|null,
				  "title": string|null,
				  "summary": string|null,
				  "years_experience": number|null,
				  "roles": string[],
				  "skills": string[],
				  "tools": string[],
				  "domains": string[],
				  "languages": [{"name": string, "level": string|null}],
				  "locations": string[],
				  "remote_preference": "remote"|"hybrid"|"onsite"|"any"|null,
				  "availability_date": string|null,
				  "education": string[],
				  "certifications": string[]
				}
				Rules:
				- If unknown, use null or empty arrays.
				- skills/tools should be normalized (e.g., "Spring Boot", "Kafka", "AWS", "PostgreSQL").
				""";

		String input = """
				You are an expert recruiter. Extract structured data from this resume text.

				%s

				RESUME TEXT:
				%s
				""".formatted(schemaHint, safeLimit(resumeText, 15000));

		ResponseCreateParams params = ResponseCreateParams.builder().model(PROFILE_MODEL).input(input).build();

		Response resp = client.responses().create(params);
		String json = stripJsonCodeFences(extractText(resp));

		try {
			return mapper.readValue(json, ResumeProfileDTO.class);
		} catch (Exception e) {
			throw new RuntimeException("Model did not return parseable JSON. Raw output:\n" + json, e);
		}
	}

	private String extractText(Response response) {
		try {
			String raw = mapper.writeValueAsString(response);
			var root = mapper.readTree(raw);

			var texts = new StringBuilder();
			collectTextFields(root, texts);

			String out = texts.toString().trim();
			if (out.isEmpty()) {
				throw new RuntimeException("No text found in response JSON. Raw response JSON: " + raw);
			}
			return out;
		} catch (Exception e) {
			throw new RuntimeException("Failed extracting text from OpenAI Response: " + e.getMessage(), e);
		}
	}

	private void collectTextFields(com.fasterxml.jackson.databind.JsonNode node, StringBuilder out) {
		if (node == null)
			return;

		if (node.isObject()) {
			node.fields().forEachRemaining(entry -> {
				String key = entry.getKey();
				var value = entry.getValue();

				if ("text".equals(key) && value.isTextual()) {
					out.append(value.asText());
				} else {
					collectTextFields(value, out);
				}
			});
		} else if (node.isArray()) {
			for (var item : node) {
				collectTextFields(item, out);
			}
		}
	}
}
