package com.taskmanager.ai.controller;

import com.taskmanager.ai.dto.AIDescriptionRequest;
import com.taskmanager.ai.dto.AIStatusRequest;
import com.taskmanager.ai.service.AITaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AITaskController}.
 * Covers Feature 1: AI Task Description Generator
 *         Feature 2: AI Status Suggester
 */
@ExtendWith(MockitoExtension.class)
class AITaskControllerTest {

    @Mock
    private AITaskService aiTaskService;

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
}


