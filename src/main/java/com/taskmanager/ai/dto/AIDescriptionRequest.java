package com.taskmanager.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for Feature 1: AI Task Description Generator.
 * The user provides only a task title — AI generates a professional description.
 */
@Getter
@Setter
public class AIDescriptionRequest {

    @NotNull(message = "Title is mandatory")
    @NotEmpty(message = "Title is mandatory")
    private String title;
}

