package com.taskmanager.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for Feature 4: AI Task Breakdown.
 *
 * <p>Provides the task title and description to the LLM so it can
 * decompose the work into 5–7 independently completable subtasks.
 */
@Getter
@Setter
public class AIBreakdownRequest {

    @NotNull(message = "Title is mandatory")
    @NotEmpty(message = "Title is mandatory")
    private String title;

    @NotNull(message = "Description is mandatory")
    @NotEmpty(message = "Description is mandatory")
    private String description;
}

