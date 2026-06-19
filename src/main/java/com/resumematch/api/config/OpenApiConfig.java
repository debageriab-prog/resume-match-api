package com.resumematch.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resumeMatchOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Resume Match API")
                .description("""
                    REST API for the Resume Matcher application.
                    
                    Provides full CRUD access to all domain entities:
                    - **Resumes** — master and tailored resume documents
                    - **Jobs** — job description documents
                    - **Improvements** — LLM-generated tailoring results linking resumes to jobs
                    - **Applications** — Kanban-style job-application tracker cards
                    - **API Keys** — encrypted LLM provider credentials
                    
                    No authentication is required in this initial version.
                    Swagger UI is available at `/swagger-ui.html`.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Resume Match API")
                    .url("https://github.com/debageriab-prog/resume-match-api"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .externalDocs(new ExternalDocumentation()
                .description("Resume Matcher project")
                .url("https://github.com/srbhr/Resume-Matcher"));
    }
}
