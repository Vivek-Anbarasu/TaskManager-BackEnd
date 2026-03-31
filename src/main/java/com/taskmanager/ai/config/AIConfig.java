package com.taskmanager.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Configuration.
 * Creates the ChatClient bean used by all AI features.
 * ChatClient.Builder is auto-configured by spring-ai-starter-model-ollama.
 *
 * <p>Also exposes an {@link ObjectMapper} bean so {@code DocumentTaskImportService}
 * (Feature 6) can parse LLM JSON responses without requiring a separate config.
 */
@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

