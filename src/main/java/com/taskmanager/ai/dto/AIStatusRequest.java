package com.taskmanager.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for Feature 2: AI Status Suggester.
 * The LLM analyses the title + description and returns the most appropriate
 * status (TODO / IN_PROGRESS / DONE / BLOCKED) with a brief reason.
 */
@Getter
@Setter
public class AIStatusRequest {

    @NotNull(message = "Title is mandatory")
    @NotEmpty(message = "Title is mandatory")
    private String title;

    @NotNull(message = "Description is mandatory")
    @NotEmpty(message = "Description is mandatory")
    private String description;
}

