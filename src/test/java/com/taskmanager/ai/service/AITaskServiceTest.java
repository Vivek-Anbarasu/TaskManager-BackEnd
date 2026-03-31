package com.taskmanager.ai.service;

import com.taskmanager.api.dto.GetTaskResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AITaskService}.
 * Covers Feature 1: AI Task Description Generator
 *         Feature 2: AI Status Suggester (including extractJson safety-net)
 *         Feature 3: AI Task Summarizer (RAG pattern)
 *
 * ChatClient is fully mocked — tests run offline without Ollama.
 *
 * LENIENT strictness is used because the Feature 3 empty-list test exits before
 * touching the LLM chain, leaving all other @BeforeEach stubs unused for that test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    // -------------------------------------------------------------------------
    // Feature 3: AI Task Summarizer (RAG Pattern)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 3 - empty task list returns fallback message without calling LLM")
    void summarizeAllTasksReturnsFallbackForEmptyList() {
        String result = aiTaskService.summarizeAllTasks(List.of());

        assertThat(result).isEqualTo("No tasks found in the system.");
        verifyNoInteractions(chatClient);   // LLM must NOT be called for empty input
    }

    @Test
    @DisplayName("Feature 3 - non-empty task list calls LLM and returns its content")
    void summarizeAllTasksCallsLLMAndReturnsContent() {
        String expectedSummary = "Project is on track. Two tasks completed, one in progress.";
        when(callSpec.content()).thenReturn(expectedSummary);

        List<GetTaskResponse> tasks = List.of(
                GetTaskResponse.builder().status("DONE").title("Add Login").description("Login page done").build(),
                GetTaskResponse.builder().status("IN_PROGRESS").title("Add Dashboard").description("WIP").build()
        );

        String result = aiTaskService.summarizeAllTasks(tasks);

        assertThat(result).isEqualTo(expectedSummary);
        verify(chatClient).prompt();
    }

    @Test
    @DisplayName("Feature 3 - prompt includes all task fields: status, title, description")
    void summarizeAllTasksPromptIncludesAllTaskData() {
        when(callSpec.content()).thenReturn("Summary here.");

        List<GetTaskResponse> tasks = List.of(
                GetTaskResponse.builder().status("BLOCKED").title("Integrate Payment API").description("Waiting for API keys").build()
        );

        aiTaskService.summarizeAllTasks(tasks);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("BLOCKED")
                .contains("Integrate Payment API")
                .contains("Waiting for API keys")
                .contains("project manager")
                .contains("Overall project health");
    }

    @Test
    @DisplayName("Feature 3 - prompt includes all 5 structured output sections")
    void summarizeAllTasksPromptRequestsAllFiveSections() {
        when(callSpec.content()).thenReturn("Summary.");

        aiTaskService.summarizeAllTasks(
                List.of(GetTaskResponse.builder().status("TODO").title("T").description("D").build()));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Overall project health")
                .contains("Completed work summary")
                .contains("Work in progress")
                .contains("Potential blockers")
                .contains("Recommended next actions");
    }

    @Test
    @DisplayName("Feature 3 - Micrometer timer recorded with correct tags")
    void summarizeAllTasksRecordsMicrometerTimer() {
        when(callSpec.content()).thenReturn("Summary.");

        aiTaskService.summarizeAllTasks(
                List.of(GetTaskResponse.builder().status("TODO").title("T").description("D").build()));

        verify(meterRegistry).timer("ai.task.summarize_all_tasks", "model", "llama3.2:1b", "feature", "task_summarizer");
        verify(timer).record(any(Supplier.class));
    }

    @Test
    @DisplayName("Feature 3 - all tasks from the list appear in the prompt")
    void summarizeAllTasksIncludesMultipleTasksInPrompt() {
        when(callSpec.content()).thenReturn("Multi-task summary.");

        List<GetTaskResponse> tasks = List.of(
                GetTaskResponse.builder().status("DONE").title("Setup CI/CD").description("Pipeline configured").build(),
                GetTaskResponse.builder().status("IN_PROGRESS").title("Write Tests").description("Unit tests in progress").build(),
                GetTaskResponse.builder().status("BLOCKED").title("Deploy to Prod").description("Waiting for approval").build()
        );

        aiTaskService.summarizeAllTasks(tasks);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt)
                .contains("Setup CI/CD")
                .contains("Write Tests")
                .contains("Deploy to Prod")
                .contains("DONE")
                .contains("IN_PROGRESS")
                .contains("BLOCKED");
    }

    // -------------------------------------------------------------------------
    // Feature 4: AI Task Breakdown (Chain-of-Thought Prompting)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 4 - should return a non-blank numbered subtask list")
    void breakdownTaskReturnsSubtaskList() {
        String expectedBreakdown = "1. Set up project structure\n2. Implement authentication\n3. Write unit tests";
        when(callSpec.content()).thenReturn(expectedBreakdown);

        String result = aiTaskService.breakdownTask(
                "Build REST API", "Create a RESTful API with JWT authentication.");

        assertThat(result).isNotBlank().isEqualTo(expectedBreakdown);
        verify(chatClient).prompt();
    }

    @Test
    @DisplayName("Feature 4 - prompt must include both title and description")
    void breakdownTaskPromptIncludesTitleAndDescription() {
        when(callSpec.content()).thenReturn("1. Define schema\n2. Implement endpoints");

        aiTaskService.breakdownTask("Design Database Schema", "Model all entities with proper relationships.");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Design Database Schema")
                .contains("Model all entities with proper relationships.");
    }

    @Test
    @DisplayName("Feature 4 - prompt must request 5-7 subtasks completable in 1-2 hours")
    void breakdownTaskPromptRequestsSubtaskScopeAndSize() {
        when(callSpec.content()).thenReturn("1. Step one");

        aiTaskService.breakdownTask("Any Task", "Any description.");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("5-7")
                .contains("1-2 hours")
                .contains("numbered list");
    }

    @Test
    @DisplayName("Feature 4 - prompt must mention senior engineer and scrum master roles")
    void breakdownTaskPromptMentionsSeniorEngineerAndScrumMaster() {
        when(callSpec.content()).thenReturn("1. Step one");

        aiTaskService.breakdownTask("Any Task", "Any description.");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("senior software engineer")
                .contains("scrum master");
    }

    @Test
    @DisplayName("Feature 4 - Micrometer timer recorded with correct tags")
    void breakdownTaskRecordsMicrometerTimer() {
        when(callSpec.content()).thenReturn("1. Step one\n2. Step two");

        aiTaskService.breakdownTask("Deploy Microservices", "Deploy all services to Kubernetes.");

        verify(meterRegistry).timer("ai.task.breakdown", "model", "llama3.2:1b", "feature", "task_breakdown");
        verify(timer).record(any(Supplier.class));
    }

    @Test
    @DisplayName("Feature 4 - should return the exact LLM content without modification")
    void breakdownTaskReturnsExactLLMContent() {
        String llmOutput = "1. Create API contract\n2. Implement controller\n3. Add validation\n4. Write tests\n5. Deploy";
        when(callSpec.content()).thenReturn(llmOutput);

        assertThat(aiTaskService.breakdownTask("Build Payments API", "Full payment flow implementation."))
                .isEqualTo(llmOutput);
    }
}


