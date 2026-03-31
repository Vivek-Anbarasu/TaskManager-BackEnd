package com.taskmanager.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Configuration.
 * Creates the ChatClient bean used by all AI features.
 * ChatClient.Builder is auto-configured by spring-ai-starter-model-ollama.
 */
@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}

