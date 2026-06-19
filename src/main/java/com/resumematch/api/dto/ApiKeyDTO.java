package com.resumematch.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Schema(description = "Encrypted LLM-provider API key record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyDTO {

    @NotBlank(message = "provider is required")
    @Schema(
        description = "Provider identifier used by the LLM router",
        example = "openai",
        allowableValues = {"openai", "anthropic", "google", "openrouter", "deepseek", "groq", "ollama", "openai_compatible"}
    )
    private String provider;

    @NotBlank(message = "ciphertext is required")
    @Schema(description = "Fernet-encrypted ciphertext of the API key (plaintext never stored)")
    private String ciphertext;

    @Schema(description = "Last-update timestamp (ISO-8601)")
    private Instant updatedAt;
}
