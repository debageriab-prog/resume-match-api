package com.resumematch.api.controller;

import com.resumematch.api.dto.ApiKeyDTO;
import com.resumematch.api.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Manage encrypted LLM provider API keys (only ciphertext is stored)")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @Operation(summary = "List all stored API key records (ciphertext only, no plaintext)")
    public ResponseEntity<List<ApiKeyDTO>> findAll() {
        return ResponseEntity.ok(apiKeyService.findAll());
    }

    @GetMapping("/{provider}")
    @Operation(summary = "Get an API key record by provider")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "API key record found"),
        @ApiResponse(responseCode = "404", description = "Provider not found", content = @Content)
    })
    public ResponseEntity<ApiKeyDTO> findByProvider(
            @Parameter(description = "Provider name (openai, anthropic, google, …)", required = true)
            @PathVariable String provider) {
        return ResponseEntity.ok(apiKeyService.findByProvider(provider));
    }

    @PutMapping("/{provider}")
    @Operation(
        summary = "Save or update an API key",
        description = "Upsert: creates the record if it doesn't exist, updates it otherwise. " +
                      "Store only the encrypted ciphertext — never plaintext."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "API key saved"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<ApiKeyDTO> save(
            @Parameter(description = "Provider name", required = true)
            @PathVariable String provider,
            @Valid @RequestBody ApiKeyDTO dto) {
        dto.setProvider(provider);
        return ResponseEntity.ok(apiKeyService.save(dto));
    }

    @DeleteMapping("/{provider}")
    @Operation(summary = "Delete an API key record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "API key deleted"),
        @ApiResponse(responseCode = "404", description = "Provider not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Provider name", required = true)
            @PathVariable String provider) {
        apiKeyService.delete(provider);
        return ResponseEntity.noContent().build();
    }
}
