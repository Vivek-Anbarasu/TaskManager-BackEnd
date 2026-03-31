package com.taskmanager.ai.controller;

import com.taskmanager.ai.dto.AIDescriptionRequest;
import com.taskmanager.ai.dto.AIStatusRequest;
import com.taskmanager.ai.service.AITaskService;
import com.taskmanager.api.dto.GetTaskResponse;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AITaskController}.
 * Covers Feature 1: AI Task Description Generator
 *         Feature 2: AI Status Suggester
 *         Feature 3: AI Task Summarizer (RAG pattern)
 */
@ExtendWith(MockitoExtension.class)
class AITaskControllerTest {

    @Mock
    private AITaskService aiTaskService;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private AITaskController aiTaskController;

    @Test
    @DisplayName("Feature 1 — generateDescription returns 200 with AI-generated description")
    void generateDescriptionReturns200WithDescription() {
        // Arrange
        AIDescriptionRequest request = new AIDescriptionRequest();
        request.setTitle("Add JWT Authentication");

        String expectedDescription = "Implement JWT-based stateless authentication.";
        when(aiTaskService.generateDescription("Add JWT Authentication"))
                .thenReturn(expectedDescription);

        // Act
        ResponseEntity<String> response = aiTaskController.generateDescription(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedDescription);
        verify(aiTaskService, times(1)).generateDescription("Add JWT Authentication");
    }

    @Test
    @DisplayName("Feature 1 — generateDescription delegates title to AITaskService")
    void generateDescriptionDelegatesToService() {
        // Arrange
        AIDescriptionRequest request = new AIDescriptionRequest();
        request.setTitle("Deploy to Kubernetes");

        when(aiTaskService.generateDescription(anyString())).thenReturn("Some AI description");

        // Act
        aiTaskController.generateDescription(request);

        // Assert
        verify(aiTaskService).generateDescription("Deploy to Kubernetes");
    }

    @Test
    @DisplayName("Feature 1 — generateDescription returns the exact service response")
    void generateDescriptionReturnsExactServiceResponse() {
        // Arrange
        AIDescriptionRequest request = new AIDescriptionRequest();
        request.setTitle("Fix Payment Gateway Bug");

        String aiResponse = "Investigate and resolve the payment gateway timeout issue "
                + "affecting checkout. This defect causes cart abandonment and revenue loss. "
                + "Acceptance criteria: payment success rate must return to 99.9%.";

        when(aiTaskService.generateDescription("Fix Payment Gateway Bug")).thenReturn(aiResponse);

        // Act
        ResponseEntity<String> response = aiTaskController.generateDescription(request);

        // Assert
        assertThat(response.getBody()).isEqualTo(aiResponse);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // Feature 2: Suggest Status
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 2 — suggestStatus returns 200 with JSON status suggestion")
    void suggestStatusReturns200WithJsonSuggestion() {
        // Arrange
        AIStatusRequest request = new AIStatusRequest();
        request.setTitle("Add Login Page");
        request.setDescription("Create the login UI with email/password validation.");

        String expectedJson = "{\"status\": \"TODO\", \"reason\": \"No work has started yet.\"}";
        when(aiTaskService.suggestStatus("Add Login Page",
                "Create the login UI with email/password validation."))
                .thenReturn(expectedJson);

        // Act
        ResponseEntity<String> response = aiTaskController.suggestStatus(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedJson);
        verify(aiTaskService, times(1)).suggestStatus(
                "Add Login Page",
                "Create the login UI with email/password validation.");
    }

    @Test
    @DisplayName("Feature 2 — suggestStatus delegates both title and description to AITaskService")
    void suggestStatusDelegatesToService() {
        // Arrange
        AIStatusRequest request = new AIStatusRequest();
        request.setTitle("Fix Payment Gateway Bug");
        request.setDescription("Payment gateway times out under load.");

        when(aiTaskService.suggestStatus(anyString(), anyString()))
                .thenReturn("{\"status\": \"IN_PROGRESS\", \"reason\": \"Work has started.\"}");

        // Act
        aiTaskController.suggestStatus(request);

        // Assert — both fields must be passed to the service
        verify(aiTaskService).suggestStatus(
                "Fix Payment Gateway Bug",
                "Payment gateway times out under load.");
    }

    @Test
    @DisplayName("Feature 2 — suggestStatus returns the exact service response")
    void suggestStatusReturnsExactServiceResponse() {
        // Arrange
        AIStatusRequest request = new AIStatusRequest();
        request.setTitle("Migrate Database");
        request.setDescription("Needs DBA sign-off before running migration scripts.");

        String expectedJson = "{\"status\": \"BLOCKED\", \"reason\": \"Waiting for DBA approval.\"}";
        when(aiTaskService.suggestStatus(anyString(), anyString())).thenReturn(expectedJson);

        // Act
        ResponseEntity<String> response = aiTaskController.suggestStatus(request);

        // Assert
        assertThat(response.getBody()).isEqualTo(expectedJson);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // Feature 3: AI Task Summarizer (RAG Pattern)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 3 — summarizeAllTasks returns 200 with AI summary")
    void summarizeAllTasksReturns200WithSummary() {
        // Arrange
        List<GetTaskResponse> tasks = List.of(
                GetTaskResponse.builder().status("DONE").title("Add Login").description("Login done").build(),
                GetTaskResponse.builder().status("IN_PROGRESS").title("Add Dashboard").description("WIP").build()
        );
        String expectedSummary = "Project is on track. 1 task completed, 1 in progress.";

        when(taskService.getAllTasks()).thenReturn(tasks);
        when(aiTaskService.summarizeAllTasks(tasks)).thenReturn(expectedSummary);

        // Act
        ResponseEntity<String> response = aiTaskController.summarizeAllTasks();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedSummary);
    }

    @Test
    @DisplayName("Feature 3 — summarizeAllTasks fetches tasks from TaskService and passes them to AITaskService")
    void summarizeAllTasksDelegatesToBothServices() {
        // Arrange
        List<GetTaskResponse> tasks = List.of(
                GetTaskResponse.builder().status("TODO").title("Write Docs").description("Documentation needed").build()
        );
        when(taskService.getAllTasks()).thenReturn(tasks);
        when(aiTaskService.summarizeAllTasks(tasks)).thenReturn("Summary here.");

        // Act
        aiTaskController.summarizeAllTasks();

        // Assert — TaskService must be called first, then AITaskService with the result
        verify(taskService, times(1)).getAllTasks();
        verify(aiTaskService, times(1)).summarizeAllTasks(tasks);
    }

    @Test
    @DisplayName("Feature 3 — summarizeAllTasks returns fallback message for empty task list")
    void summarizeAllTasksReturnsFallbackForEmptyList() {
        // Arrange
        when(taskService.getAllTasks()).thenReturn(List.of());
        when(aiTaskService.summarizeAllTasks(List.of())).thenReturn("No tasks found in the system.");

        // Act
        ResponseEntity<String> response = aiTaskController.summarizeAllTasks();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("No tasks found in the system.");
    }

    @Test
    @DisplayName("Feature 3 — summarizeAllTasks returns the exact AITaskService response")
    void summarizeAllTasksReturnsExactServiceResponse() {
        // Arrange
        String exactSummary = "1. Health: Good. 2. Completed: Auth module. 3. In Progress: Dashboard. "
                + "4. Blockers: None. 5. Next: Deploy to staging.";
        when(taskService.getAllTasks()).thenReturn(List.of(
                GetTaskResponse.builder().status("DONE").title("Auth Module").description("JWT implemented").build()
        ));
        when(aiTaskService.summarizeAllTasks(anyList())).thenReturn(exactSummary);

        // Act
        ResponseEntity<String> response = aiTaskController.summarizeAllTasks();

        // Assert
        assertThat(response.getBody()).isEqualTo(exactSummary);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}

