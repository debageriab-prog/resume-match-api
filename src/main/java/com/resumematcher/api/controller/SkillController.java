package com.resumematcher.api.controller;

import com.resumematcher.api.entity.Skill;
import com.resumematcher.api.entity.enums.SkillCategory;
import com.resumematcher.api.service.SkillService;
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
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@Tag(name = "Skills", description = "Manage skill tags")
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    @Operation(summary = "Get all skills")
    public ResponseEntity<List<Skill>> getAll() {
        return ResponseEntity.ok(skillService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get skill by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Skill found"),
        @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public ResponseEntity<Skill> getById(
            @Parameter(description = "Skill ID") @PathVariable Long id) {
        return ResponseEntity.ok(skillService.findById(id));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get skills by category")
    public ResponseEntity<List<Skill>> getByCategory(
            @Parameter(description = "Skill category") @PathVariable SkillCategory category) {
        return ResponseEntity.ok(skillService.findByCategory(category));
    }

    @PostMapping
    @Operation(summary = "Create a new skill")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Skill created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Skill already exists")
    })
    public ResponseEntity<Skill> create(@Valid @RequestBody Skill skill) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.create(skill));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a skill")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Skill updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Skill not found"),
        @ApiResponse(responseCode = "409", description = "Skill name already in use")
    })
    public ResponseEntity<Skill> update(
            @Parameter(description = "Skill ID") @PathVariable Long id,
            @Valid @RequestBody Skill skill) {
        return ResponseEntity.ok(skillService.update(id, skill));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a skill")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Skill deleted"),
        @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Skill ID") @PathVariable Long id) {
        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
