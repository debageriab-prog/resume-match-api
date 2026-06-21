package se.debageri.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.openai.client.OpenAIClient;

import se.debageri.api.rabbit.AssignmentEventPublisher;

@SpringBootTest
@ActiveProfiles("test")
class ResumeMatchApiApplicationTests {

	@MockBean
	private OpenAIClient openAIClient;

	@MockBean
	private AssignmentEventPublisher assignmentEventPublisher;

	@Test
	void contextLoads() {
	}
}
