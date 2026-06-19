package com.resumematcher.api.controller;

import com.resumematcher.api.entity.Company;
import com.resumematcher.api.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Manage companies posting jobs")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "Get all companies")
    public ResponseEntity<List<Company>> getAll(
            @Parameter(description = "Filter by name (partial match)") @RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(companyService.searchByName(name));
        }
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company found"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<Company> getById(
            @Parameter(description = "Company ID") @PathVariable Long id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new company")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Company created"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<Company> create(@Valid @RequestBody Company company) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(company));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a company")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<Company> update(
            @Parameter(description = "Company ID") @PathVariable Long id,
            @Valid @RequestBody Company company) {
        return ResponseEntity.ok(companyService.update(id, company));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a company")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Company deleted"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Company ID") @PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
