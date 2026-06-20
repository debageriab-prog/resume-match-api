package se.debageri.api.service;

import static se.debageri.api.util.StringUtil.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.EmbeddingModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import se.debageri.api.dto.AssignmentSeekerInfoDTO;
import se.debageri.api.dto.FitEvaluationDTO;
import se.debageri.api.dto.ResumeProfileDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenAiService {

	private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

	private final OpenAIClient client;
	private final ObjectMapper mapper;

	private static final EmbeddingModel EMBEDDING_MODEL = EmbeddingModel.TEXT_EMBEDDING_3_SMALL;

	public static final String PROFILE_MODEL = "gpt-4o";

	public static final String JUDGE_MODEL = "gpt-4o-mini";

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

	/**
	 * Generates a dense vector embedding for the given text.
	 *
	 * @param text
	 *            the input text to embed
	 * @return a {@code float[]} representing the embedding vector
	 */
	public float[] embed(String text) {
		EmbeddingCreateParams params = EmbeddingCreateParams.builder().model(EMBEDDING_MODEL).input(text).build();

		CreateEmbeddingResponse resp = client.embeddings().create(params);

		List<Float> vec = resp.data().getFirst().embedding();

		float[] out = new float[vec.size()];
		for (int i = 0; i < vec.size(); i++) {
			out[i] = vec.get(i);
		}
		return out;
	}

	/**
	 * Evaluates how well a candidate fits an assignment using the judge LLM.
	 *
	 * @param resumeProfileJson
	 *            the candidate's structured profile serialized as JSON
	 * @param assignmentText
	 *            the full text of the assignment/job description
	 * @param assignmentTitle
	 *            optional title of the assignment; may be {@code null}
	 * @return a {@link FitEvaluationDTO} containing a fit score, decision, missing
	 *         must-haves, and reasons
	 * @throws RuntimeException
	 *             if the model response cannot be parsed as JSON
	 */
	public FitEvaluationDTO evaluateFit(String resumeProfileJson, String assignmentText, String assignmentTitle) {
		String schema = """
				Return ONLY valid JSON. No markdown, no extra text.
				Schema:
				{
				  "fit": integer,              // 0..100
				  "decision": string,          // "strong_yes"|"yes"|"maybe"|"no"
				  "missing_must_haves": string[],
				  "reasons": string[]          // short bullet-like reasons (max 6)
				}
				Rules:
				- Be strict about must-have skills. If core stack differs, fit should be low (<40).
				- Consider seniority/role mismatch (e.g., Lead Architect vs Senior Developer).
				- DO not Consider location.
				- Keep reasons concise, also keep reason in English even if input is another language.
				- Try to mention missing must-have if possible instead of put everything in reason section.
				- You must make a decision on every candidate, empty decision is not allowed.
				""";

		String input = """
				You are a strict technical recruiter.
				Evaluate how well this candidate fits the assignment.
				Return ONLY a raw JSON object. Do NOT wrap in ``` or markdown. Output must start with '{' and end with '}'.

				%s

				CANDIDATE_PROFILE_JSON:
				%s

				ASSIGNMENT_TITLE:
				%s

				ASSIGNMENT_TEXT:
				%s
				"""
				.formatted(schema, safeLimit(resumeProfileJson, 12000), assignmentTitle == null ? "" : assignmentTitle,
						safeLimit(assignmentText, 12000));

		ResponseCreateParams params = ResponseCreateParams.builder().model(JUDGE_MODEL).input(input).build();

		Response resp = client.responses().create(params);
		String json = stripJsonCodeFences(extractText(resp));
		log.trace("OpenAI Fit evaluation JSON: {}", json);
		try {
			return mapper.readValue(json, FitEvaluationDTO.class);
		} catch (Exception e) {
			throw new RuntimeException("Fit evaluation JSON was not parseable. Raw:\n" + json, e);
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
