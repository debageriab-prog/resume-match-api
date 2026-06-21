package se.debageri.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI resumeMatchApiOpenAPI() {
		return new OpenAPI().info(new Info().title("Resume Match API").description(
				"REST API for managing assignments, seekers, resumes, and match results from the resume-matcher application.")
				.version("1.0.0")
				.contact(new Contact().name("debageri").url("https://github.com/debageriab-prog/resume-match-api"))
				.license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
	}
}
