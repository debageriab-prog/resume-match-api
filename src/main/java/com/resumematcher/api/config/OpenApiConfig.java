package com.resumematcher.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resumeMatchApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resume Match API")
                        .description("REST API for managing resumes, candidates, job postings, and match results. " +
                                "Designed to serve as a backend for a resume-matching front-end application.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Resume Matcher")
                                .url("https://github.com/debageriab-prog/resume-match-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
