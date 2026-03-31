package com.taskmanager.ai.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
/**
 * Unit tests for {@link AITaskService}.
 * Covers Feature 1: AI Task Description Generator
 *         Feature 2: AI Status Suggester (including extractJson safety-net)
 *
 * ChatClient is fully mocked — tests run offline without Ollama.
 */
@ExtendWith(MockitoExtension.class)
class AITaskServiceTest {
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callSpec;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Timer timer;
    private AITaskService aiTaskService;
    @BeforeEach
    void setUp() {
        aiTaskService = new AITaskService(chatClient, meterRegistry);
        when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(timer);
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        }).when(timer).record(any(Supplier.class));
        when(chatClient.prompt()).thenReturn(requestSpec);
        lenient().when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }
    // Feature 1: Generate Description
    @Test
    @DisplayName("Feature 1 - should generate a non-blank description from a title")
    void shouldGenerateDescription() {
        String expectedDescription = "Implement a secure JWT token authentication flow.";
        when(callSpec.content()).thenReturn(expectedDescription);
        String result = aiTaskService.generateDescription("Add JWT Authentication");
        assertThat(result).isNotBlank().isEqualTo(expectedDescription);
        verify(chatClient).prompt();
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Add JWT Authentication")
                .contains("project management assistant");
    }
    @Test
    @DisplayName("Feature 1 - prompt must include the task title")
    void promptMustIncludeTitle() {
        when(callSpec.content()).thenReturn("Some description");
        aiTaskService.generateDescription("Fix Login Bug");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("Fix Login Bug");
    }
    @Test
    @DisplayName("Feature 1 - should call Micrometer timer with correct tags")
    void shouldRecordMetricsWithCorrectTags() {
        when(callSpec.content()).thenReturn("Generated description");
        aiTaskService.generateDescription("Deploy to Kubernetes");
        verify(meterRegistry).timer("ai.task.generate_description", "model", "llama3.2:1b", "feature", "description_generator");
        verify(timer).record(any(Supplier.class));
    }
    @Test
    @DisplayName("Feature 1 - should return the exact LLM content without modification")
    void shouldReturnExactLLMContent() {
        String llmOutput = "This is exactly what the LLM returned.";
        when(callSpec.content()).thenReturn(llmOutput);
        assertThat(aiTaskService.generateDescription("Any title")).isEqualTo(llmOutput);
    }
    // Feature 2: Suggest Status
    @Test
    @DisplayName("Feature 2 - should return a JSON status suggestion")
    void shouldSuggestStatus() {
        String expectedJson = "{\"status\": \"TODO\", \"reason\": \"No work has started yet.\"}";
        when(callSpec.content()).thenReturn(expectedJson);
        String result = aiTaskService.suggestStatus("Add Login Page", "Create the login UI with validation.");
        assertThat(result).isNotBlank().contains("TODO").contains("reason");
        verify(chatClient).prompt();
    }
    @Test
    @DisplayName("Feature 2 - prompt must include both title and description")
    void suggestStatusPromptMustIncludeTitleAndDescription() {
        when(callSpec.content()).thenReturn("{\"status\": \"IN_PROGRESS\", \"reason\": \"Work begun.\"}");
        aiTaskService.suggestStatus("Fix Payment Bug", "Payment gateway times out under load.");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Fix Payment Bug")
                .contains("Payment gateway times out under load.")
                .contains("BLOCKED");
    }
    @Test
    @DisplayName("Feature 2 - must call .system() with a JSON-only instruction")
    void suggestStatusMustCallSystemWithJsonOnlyInstruction() {
        when(callSpec.content()).thenReturn("{\"status\": \"TODO\", \"reason\": \"Not started.\"}");
        aiTaskService.suggestStatus("Any Title", "Any description.");
        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).system(systemCaptor.capture());
        assertThat(systemCaptor.getValue())
                .containsIgnoringCase("JSON")
                .containsIgnoringCase("ONLY");
    }
    @Test
    @DisplayName("Feature 2 - should call Micrometer timer with correct tags")
    void suggestStatusShouldRecordMetricsWithCorrectTags() {
        when(callSpec.content()).thenReturn("{\"status\": \"DONE\", \"reason\": \"Completed.\"}");
        aiTaskService.suggestStatus("Deploy to Prod", "Final deployment step.");
        verify(meterRegistry).timer("ai.task.suggest_status", "model", "llama3.2:1b", "feature", "status_suggester");
        verify(timer).record(any(Supplier.class));
    }

    @Test
    @DisplayName("Feature 2 - BLOCKED scenario: developer cannot proceed due to internet issue")
    void suggestStatusBlockedWhenDeveloperCannotProceedDueToInternetIssue() {
        String description = "Use the Google API endpoint and get the data and store in the database.\n"
                + "Acceptance Criteria: Integrate the API, https://google.com/getTasks.\n"
                + "Developer Comments: I am not able to proceed development as Internet is not working.";
        when(callSpec.content()).thenReturn(
                "{\"status\": \"BLOCKED\", \"reason\": \"Developer cannot proceed — internet is unavailable.\"}");
        String result = aiTaskService.suggestStatus("Integrate Google Tasks API", description);
        assertThat(result).contains("BLOCKED");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("<STATUS>")
                .doesNotContain("\"status\": \"TODO\"")
                .contains("not able to")
                .contains("Internet, network, or environment is unavailable")
                .contains("Integrate Google Tasks API");
    }
    @Test
    @DisplayName("Feature 2 - valid statuses and JSON-only instruction are in the prompt")
    void suggestStatusPromptConstrainsValidStatuses() {
        when(callSpec.content()).thenReturn("{\"status\": \"TODO\", \"reason\": \"Not started.\"}");
        aiTaskService.suggestStatus("Write Unit Tests", "Cover all service methods.");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("TODO")
                .contains("IN_PROGRESS")
                .contains("DONE")
                .contains("BLOCKED")
                .contains("<STATUS>")
                .contains("ONLY this JSON");
    }
}
