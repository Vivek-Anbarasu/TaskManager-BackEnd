package com.taskmanager.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for Feature 5: AI Conversational Chatbot.
 *
 * <p>Carries the user's natural-language question about their tasks,
 * e.g. "How many tasks are IN_PROGRESS?" or "What should I work on next?"
 */
@Getter
@Setter
public class ChatRequest {

    @NotEmpty(message = "Message cannot be empty")
    private String message;
}

