package com.taskmanager.ai.controller;

import com.taskmanager.ai.dto.AIBreakdownRequest;
import com.taskmanager.ai.dto.AIDescriptionRequest;
import com.taskmanager.ai.dto.AIStatusRequest;
import com.taskmanager.ai.dto.ChatRequest;
import com.taskmanager.ai.dto.ChatResponse;
import com.taskmanager.ai.dto.ImportDocumentResponse;
import com.taskmanager.ai.service.DocumentTaskImportService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AITaskController}.
 * Covers Feature 1: AI Task Description Generator
 *         Feature 2: AI Status Suggester
 *         Feature 3: AI Task Summarizer (RAG pattern)
 *         Feature 4: AI Task Breakdown (Chain-of-Thought Prompting)
 *         Feature 5: AI Conversational Chatbot (RAG + Conversational AI)
 *         Feature 6: Document Ingestion — PDF/Word → Tasks
 */
@ExtendWith(MockitoExtension.class)
class AITaskControllerTest {

    @Mock private AITaskService aiTaskService;
    @Mock private TaskService taskService;
    @Mock private DocumentTaskImportService documentTaskImportService;
    @Mock private MultipartFile multipartFile;

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

    // -------------------------------------------------------------------------
    // Feature 4: AI Task Breakdown (Chain-of-Thought Prompting)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 4 — breakdownTask returns 200 with subtask list")
    void breakdownTaskReturns200WithSubtaskList() {
        // Arrange
        AIBreakdownRequest request = new AIBreakdownRequest();
        request.setTitle("Build REST API");
        request.setDescription("Create a RESTful API with authentication and CRUD operations.");

        String expectedBreakdown = "1. Define API contract\n2. Implement controllers\n3. Add validation\n"
                + "4. Write unit tests\n5. Deploy to staging";
        when(aiTaskService.breakdownTask("Build REST API",
                "Create a RESTful API with authentication and CRUD operations."))
                .thenReturn(expectedBreakdown);

        // Act
        ResponseEntity<String> response = aiTaskController.breakdownTask(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedBreakdown);
        verify(aiTaskService, times(1)).breakdownTask(
                "Build REST API",
                "Create a RESTful API with authentication and CRUD operations.");
    }

    @Test
    @DisplayName("Feature 4 — breakdownTask delegates both title and description to AITaskService")
    void breakdownTaskDelegatesToService() {
        // Arrange
        AIBreakdownRequest request = new AIBreakdownRequest();
        request.setTitle("Implement Payment Gateway");
        request.setDescription("Integrate Stripe API with webhook handling and retry logic.");

        when(aiTaskService.breakdownTask(anyString(), anyString()))
                .thenReturn("1. Set up Stripe SDK\n2. Implement payment intent creation");

        // Act
        aiTaskController.breakdownTask(request);

        // Assert — both fields must be forwarded to the service
        verify(aiTaskService).breakdownTask(
                "Implement Payment Gateway",
                "Integrate Stripe API with webhook handling and retry logic.");
    }

    @Test
    @DisplayName("Feature 4 — breakdownTask returns the exact AITaskService response")
    void breakdownTaskReturnsExactServiceResponse() {
        // Arrange
        AIBreakdownRequest request = new AIBreakdownRequest();
        request.setTitle("Migrate to Microservices");
        request.setDescription("Extract monolith modules into independent deployable services.");

        String exactBreakdown = "1. Identify bounded contexts\n2. Define service APIs\n"
                + "3. Extract user service\n4. Extract order service\n"
                + "5. Set up API gateway\n6. Configure service discovery\n7. Write integration tests";
        when(aiTaskService.breakdownTask(anyString(), anyString())).thenReturn(exactBreakdown);

        // Act
        ResponseEntity<String> response = aiTaskController.breakdownTask(request);

        // Assert
        assertThat(response.getBody()).isEqualTo(exactBreakdown);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // Feature 5: AI Conversational Chatbot (RAG + Conversational AI)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 5 — chat returns 200 with ChatResponse containing reply and tasksAnalyzed")
    void chatReturns200WithChatResponse() {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setMessage("How many tasks are IN_PROGRESS?");

        List<GetTaskResponse> tasks = List.of(
                GetTaskResponse.builder().id(1L).status("IN_PROGRESS").title("Add Dashboard").description("WIP").build()
        );
        ChatResponse expectedResponse = ChatResponse.builder()
                .reply("There is 1 IN_PROGRESS task: Add Dashboard.")
                .tasksAnalyzed(1)
                .build();

        when(taskService.getAllTasks()).thenReturn(tasks);
        when(aiTaskService.chat("How many tasks are IN_PROGRESS?", tasks)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<ChatResponse> response = aiTaskController.chat(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReply()).isEqualTo("There is 1 IN_PROGRESS task: Add Dashboard.");
        assertThat(response.getBody().getTasksAnalyzed()).isEqualTo(1);
    }

    @Test
    @DisplayName("Feature 5 — chat fetches all tasks then delegates message + tasks to AITaskService")
    void chatFetchesAllTasksAndDelegatesToAIService() {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setMessage("Are there any blocked tasks?");

        List<GetTaskResponse> tasks = List.of(
                GetTaskResponse.builder().id(1L).status("BLOCKED").title("Deploy to Prod").description("Needs approval").build()
        );
        when(taskService.getAllTasks()).thenReturn(tasks);
        when(aiTaskService.chat(anyString(), anyList()))
                .thenReturn(ChatResponse.builder().reply("Yes, 1 task is blocked.").tasksAnalyzed(1).build());

        // Act
        aiTaskController.chat(request);

        // Assert — TaskService must be called first, then AITaskService with both args
        verify(taskService, times(1)).getAllTasks();
        verify(aiTaskService, times(1)).chat("Are there any blocked tasks?", tasks);
    }

    @Test
    @DisplayName("Feature 5 — chat returns the exact AITaskService ChatResponse")
    void chatReturnsExactServiceResponse() {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setMessage("Give me a standup summary.");

        ChatResponse exactResponse = ChatResponse.builder()
                .reply("2 tasks done, 1 in progress, 0 blocked. Looking good!")
                .tasksAnalyzed(3)
                .build();
        when(taskService.getAllTasks()).thenReturn(List.of());
        when(aiTaskService.chat(anyString(), anyList())).thenReturn(exactResponse);

        // Act
        ResponseEntity<ChatResponse> response = aiTaskController.chat(request);

        // Assert
        assertThat(response.getBody()).isEqualTo(exactResponse);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Feature 5 — chat passes empty task list to AITaskService when no tasks exist")
    void chatPassesEmptyTaskListWhenNoTasksExist() {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setMessage("What should I work on?");

        when(taskService.getAllTasks()).thenReturn(List.of());
        when(aiTaskService.chat(anyString(), anyList()))
                .thenReturn(ChatResponse.builder()
                        .reply("I don't have enough information to answer that.")
                        .tasksAnalyzed(0)
                        .build());

        // Act
        ResponseEntity<ChatResponse> response = aiTaskController.chat(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(aiTaskService).chat("What should I work on?", List.of());
    }

    // -------------------------------------------------------------------------
    // Feature 6: Document Ingestion — PDF/Word → Tasks
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 6 — importDocument returns 200 with ImportDocumentResponse")
    void importDocumentReturns200WithImportDocumentResponse() throws Exception {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("sprint-plan.pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(documentTaskImportService.importTasksFromDocument(multipartFile))
                .thenReturn(List.of(1L, 2L, 3L));

        // Act
        ResponseEntity<ImportDocumentResponse> response = aiTaskController.importDocument(multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTaskIds()).containsExactly(1L, 2L, 3L);
        assertThat(response.getBody().getMessage()).contains("3");
    }

    @Test
    @DisplayName("Feature 6 — importDocument delegates to DocumentTaskImportService")
    void importDocumentDelegatesToDocumentTaskImportService() throws Exception {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(2048L);
        when(multipartFile.getOriginalFilename()).thenReturn("requirements.docx");
        when(documentTaskImportService.importTasksFromDocument(multipartFile))
                .thenReturn(List.of(10L, 11L));

        // Act
        aiTaskController.importDocument(multipartFile);

        // Assert — service must be called with the file
        verify(documentTaskImportService, times(1)).importTasksFromDocument(multipartFile);
    }

    @Test
    @DisplayName("Feature 6 — importDocument throws BadRequest when file is empty")
    void importDocumentThrowsBadRequestForEmptyFile() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(true);

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                com.taskmanager.exception.BadRequest.class,
                () -> aiTaskController.importDocument(multipartFile));

        verifyNoInteractions(documentTaskImportService);
    }

    @Test
    @DisplayName("Feature 6 — importDocument returns the exact list of saved task IDs")
    void importDocumentReturnsExactSavedTaskIds() throws Exception {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(512L);
        when(multipartFile.getOriginalFilename()).thenReturn("notes.pdf");
        List<Long> expectedIds = List.of(100L, 200L, 300L, 400L, 500L);
        when(documentTaskImportService.importTasksFromDocument(multipartFile))
                .thenReturn(expectedIds);

        // Act
        ResponseEntity<ImportDocumentResponse> response = aiTaskController.importDocument(multipartFile);

        // Assert
        assertThat(response.getBody().getTaskIds()).isEqualTo(expectedIds);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Feature 6 — importDocument throws BadRequest for unsupported file type (e.g. .exe)")
    void importDocumentThrowsBadRequestForUnsupportedFileType() {
        // Arrange — browser uploads a file with an extension not in the allowed list
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("malware.exe");

        // Act & Assert — service must never be called
        org.junit.jupiter.api.Assertions.assertThrows(
                com.taskmanager.exception.BadRequest.class,
                () -> aiTaskController.importDocument(multipartFile));

        verifyNoInteractions(documentTaskImportService);
    }

    @Test
    @DisplayName("Feature 6 — importDocument accepts .xlsx file and delegates to service")
    void importDocumentAcceptsXlsxFile() throws Exception {
        // Arrange — backlog-tasks.xlsx from the sample-documents folder
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("backlog-tasks.xlsx");
        when(multipartFile.getSize()).thenReturn(3809L);
        when(documentTaskImportService.importTasksFromDocument(multipartFile))
                .thenReturn(List.of(1L, 2L, 3L));

        // Act
        ResponseEntity<ImportDocumentResponse> response = aiTaskController.importDocument(multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTaskIds()).containsExactly(1L, 2L, 3L);
        verify(documentTaskImportService).importTasksFromDocument(multipartFile);
    }

    @Test
    @DisplayName("Feature 6 — importDocument accepts .docx file and delegates to service")
    void importDocumentAcceptsDocxFile() throws Exception {
        // Arrange — sprint-tasks.docx from the sample-documents folder
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("sprint-tasks.docx");
        when(multipartFile.getSize()).thenReturn(2749L);
        when(documentTaskImportService.importTasksFromDocument(multipartFile))
                .thenReturn(List.of(7L, 8L));

        // Act
        ResponseEntity<ImportDocumentResponse> response = aiTaskController.importDocument(multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTaskIds()).containsExactly(7L, 8L);
        verify(documentTaskImportService).importTasksFromDocument(multipartFile);
    }
}

