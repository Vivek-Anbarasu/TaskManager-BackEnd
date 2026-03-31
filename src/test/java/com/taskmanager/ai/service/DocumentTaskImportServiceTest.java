package com.taskmanager.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.domain.model.Tasks;
import com.taskmanager.domain.repository.TaskRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentTaskImportService} — Feature 6: Document Ingestion.
 *
 * <p>{@code extractText} is stubbed via {@code Mockito.spy()} (the method is {@code protected}
 * in the service) so tests run without actual PDF/Word bytes or Spring AI document readers.
 *
 * <p>The real {@link ObjectMapper} is used to verify JSON parsing logic end-to-end.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentTaskImportServiceTest {

    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callSpec;
    @Mock private TaskRepository taskRepository;
    @Mock private MultipartFile multipartFile;

    // Use a real ObjectMapper — we want to verify JSON parsing end-to-end
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DocumentTaskImportService spyService;

    @BeforeEach
    void setUp() throws Exception {
        DocumentTaskImportService service =
                new DocumentTaskImportService(chatClient, taskRepository, objectMapper);
        spyService = spy(service);

        // Stub ChatClient fluent chain (service now calls .system().user().call().content())
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);

        // Stub extractText so tests run without actual document bytes
        doReturn("Sample sprint planning document with action items.")
                .when(spyService).extractText(any(MultipartFile.class));

        // Default MultipartFile metadata
        when(multipartFile.getOriginalFilename()).thenReturn("sprint-plan.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
    }

    // -------------------------------------------------------------------------
    // Happy-path: end-to-end flow
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 6 - should parse LLM JSON, save all tasks, and return their IDs")
    void importTasksParsesLLMJsonSavesTasksAndReturnsIds() throws Exception {
        // Arrange — LLM returns a clean JSON array with two tasks
        String llmJson = """
                [
                  {"title": "Setup CI pipeline",  "description": "Configure GitLab CI/CD", "status": "TODO"},
                  {"title": "Fix login bug",       "description": "NPE in AuthController",  "status": "IN_PROGRESS"}
                ]
                """;
        when(callSpec.content()).thenReturn(llmJson);

        Tasks saved1 = Tasks.builder().taskId(10L).title("Setup CI pipeline").build();
        Tasks saved2 = Tasks.builder().taskId(11L).title("Fix login bug").build();
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(saved1, saved2));

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert
        assertThat(ids).containsExactly(10L, 11L);
        verify(taskRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Feature 6 - should call TaskRepository.saveAll with correctly mapped Task entities")
    void importTasksSavesEntitiesWithCorrectFieldsFromLLMJson() throws Exception {
        // Arrange
        String llmJson = """
                [{"title": "Write API docs", "description": "Document all REST endpoints", "status": "TODO"}]
                """;
        when(callSpec.content()).thenReturn(llmJson);
        when(taskRepository.saveAll(anyList()))
                .thenReturn(List.of(Tasks.builder().taskId(42L).build()));

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert — capture what was passed to saveAll and verify each field
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tasks>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        List<Tasks> savedTasks = captor.getValue();
        assertThat(savedTasks).hasSize(1);
        assertThat(savedTasks.get(0).getTitle()).isEqualTo("Write API docs");
        assertThat(savedTasks.get(0).getDescription()).isEqualTo("Document all REST endpoints");
        assertThat(savedTasks.get(0).getStatus()).isEqualTo("TODO");
    }

    @Test
    @DisplayName("Feature 6 - prompt must include the extracted document text inside <document> tags")
    void importTasksPromptContainsExtractedDocumentText() throws Exception {
        // Arrange
        doReturn("Sprint 12 tasks: 1. Deploy to prod. 2. Fix payment bug.")
                .when(spyService).extractText(any(MultipartFile.class));
        when(callSpec.content()).thenReturn("[]");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert — the extracted text must appear verbatim in the user prompt inside <document> tags
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Sprint 12 tasks: 1. Deploy to prod. 2. Fix payment bug.")
                .contains("<document>")
                .contains("</document>")
                .contains("Extract ALL tasks");
    }

    @Test
    @DisplayName("Feature 6 - prompt must contain CRITICAL RULES preventing example echo")
    void importTasksPromptContainsCriticalRulesAgainstHallucination() throws Exception {
        // Arrange
        when(callSpec.content()).thenReturn("[]");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert — anti-hallucination guardrails and single-array rule must be present
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("CRITICAL RULES")
                .contains("do NOT invent")
                .contains("Do NOT copy")
                .contains("ONE single JSON array")          // rule added for multi-array bug fix
                .doesNotContain("Setup CI pipeline")        // concrete example must NOT appear
                .doesNotContain("Fix login bug");           // concrete example must NOT appear
    }

    @Test
    @DisplayName("Feature 6 - prompt must enforce exact uppercase status strings (TODO not 'To Do')")
    void importTasksPromptEnforcesUppercaseStatusStrings() throws Exception {
        // Arrange
        when(callSpec.content()).thenReturn("[]");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert — prompt must reject natural-language status variations
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("TODO")
                .contains("IN_PROGRESS")
                .contains("DONE")
                .contains("BLOCKED")
                .contains("Do NOT write")
                .contains("To Do")       // must appear as a forbidden example
                .contains("In Progress"); // must appear as a forbidden example
    }

    @Test
    @DisplayName("Feature 6 - merges multiple separate JSON arrays (one-per-task) into one list")
    void importTasksMergesMultipleSeparateJsonArraysIntoOne() throws Exception {
        // This is the exact failure mode observed in production:
        // LLM returned 5 separate single-element arrays → Jackson only parsed the first → 1/5 saved.
        String multipleArraysResponse =
                "[{\"title\": \"Setup CI\",      \"description\": \"Configure pipeline\", \"status\": \"TODO\"}]\n"
              + "[{\"title\": \"Fix login bug\",  \"description\": \"NPE in controller\",  \"status\": \"IN_PROGRESS\"}]\n"
              + "[{\"title\": \"Write API docs\", \"description\": \"Document endpoints\", \"status\": \"TODO\"}]";

        when(callSpec.content()).thenReturn(multipleArraysResponse);
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(
                Tasks.builder().taskId(1L).title("Setup CI").build(),
                Tasks.builder().taskId(2L).title("Fix login bug").build(),
                Tasks.builder().taskId(3L).title("Write API docs").build()
        ));

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert — all 3 tasks must be saved, not just the first one
        assertThat(ids).hasSize(3).containsExactly(1L, 2L, 3L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tasks>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        List<Tasks> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved.get(0).getTitle()).isEqualTo("Setup CI");
        assertThat(saved.get(1).getTitle()).isEqualTo("Fix login bug");
        assertThat(saved.get(2).getTitle()).isEqualTo("Write API docs");
    }

    @Test
    @DisplayName("Feature 6 - merges five separate arrays (exact reproduction of reported production bug)")
    void importTasksMergesFiveSeparateArraysMatchingProductionBug() throws Exception {
        // Reproduces the exact LLM output that caused 1/5 tasks to be saved
        String fiveArrays =
                "[{\"title\":\"Configure CI/CD Pipeline\",\"description\":\"Set up GitHub Actions\",\"status\":\"TODO\"}]\n"
              + "[{\"title\":\"Implement JWT Auth\",\"description\":\"Stateless JWT tokens\",\"status\":\"IN_PROGRESS\"}]\n"
              + "[{\"title\":\"Design DB Schema\",\"description\":\"Normalised PostgreSQL schema\",\"status\":\"TODO\"}]\n"
              + "[{\"title\":\"90% Test Coverage\",\"description\":\"JUnit 5 + JaCoCo\",\"status\":\"TODO\"}]\n"
              + "[{\"title\":\"SendGrid Integration\",\"description\":\"Transactional emails\",\"status\":\"BLOCKED\"}]";

        when(callSpec.content()).thenReturn(fiveArrays);
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(
                Tasks.builder().taskId(10L).build(), Tasks.builder().taskId(11L).build(),
                Tasks.builder().taskId(12L).build(), Tasks.builder().taskId(13L).build(),
                Tasks.builder().taskId(14L).build()
        ));

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert — ALL 5 tasks saved, not just the first
        assertThat(ids).hasSize(5).containsExactly(10L, 11L, 12L, 13L, 14L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tasks>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(5);
    }

    @Test
    @DisplayName("Feature 6 - system prompt must instruct model to output ONLY raw JSON")
    void importTasksSystemPromptInstructsJsonOnlyOutput() throws Exception {
        // Arrange
        when(callSpec.content()).thenReturn("[]");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert — system prompt enforces JSON-only output
        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).system(systemCaptor.capture());
        assertThat(systemCaptor.getValue())
                .containsIgnoringCase("JSON")
                .containsIgnoringCase("ONLY")
                .containsIgnoringCase("NEVER");
    }

    @Test
    @DisplayName("Feature 6 - extractJsonArray strips narrative prose and returns only the JSON array")
    void importTasksHandlesLLMResponseWithNarrativeAroundJsonArray() throws Exception {
        // Arrange — LLM wraps the JSON array in prose (common in small models)
        String narrativeResponse = "Based on the document, here are the extracted tasks:\n"
                + "[{\"title\": \"Deploy service\", \"description\": \"Deploy to K8s\", \"status\": \"TODO\"}]\n"
                + "I hope this helps!";
        when(callSpec.content()).thenReturn(narrativeResponse);
        when(taskRepository.saveAll(anyList()))
                .thenReturn(List.of(Tasks.builder().taskId(99L).build()));

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert — JSON was extracted correctly despite narrative wrapper
        assertThat(ids).containsExactly(99L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tasks>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getTitle()).isEqualTo("Deploy service");
    }

    @Test
    @DisplayName("Feature 6 - returns empty list when LLM extracts no tasks (blank JSON array)")
    void importTasksReturnsEmptyListWhenLLMFindsNoTasks() throws Exception {
        // Arrange — LLM returns empty array
        when(callSpec.content()).thenReturn("[]");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert
        assertThat(ids).isEmpty();
        verify(taskRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("Feature 6 - returns empty list when LLM response contains no JSON array")
    void importTasksReturnsFallbackEmptyArrayOnBlankLLMResponse() throws Exception {
        // Arrange — LLM returns blank response (no JSON array at all)
        when(callSpec.content()).thenReturn("");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert — fallback to "[]" prevents Jackson parse failure
        assertThat(ids).isEmpty();
    }

    @Test
    @DisplayName("Feature 6 - extractText is called once with the uploaded file")
    void importTasksCallsExtractTextWithUploadedFile() throws Exception {
        // Arrange
        when(callSpec.content()).thenReturn("[]");
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert
        verify(spyService).extractText(multipartFile);
    }

    @Test
    @DisplayName("Feature 6 - saves multiple tasks and returns all their IDs in order")
    void importTasksReturnsAllSavedIdsInOrder() throws Exception {
        // Arrange — three tasks extracted from document
        String llmJson = """
                [
                  {"title": "Task A", "description": "Desc A", "status": "TODO"},
                  {"title": "Task B", "description": "Desc B", "status": "IN_PROGRESS"},
                  {"title": "Task C", "description": "Desc C", "status": "DONE"}
                ]
                """;
        when(callSpec.content()).thenReturn(llmJson);
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(
                Tasks.builder().taskId(1L).build(),
                Tasks.builder().taskId(2L).build(),
                Tasks.builder().taskId(3L).build()
        ));

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert
        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    // -------------------------------------------------------------------------
    // Status normalisation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 6 - normalizeStatus corrects all LLM status variants before saving to DB")
    void importTasksNormalizesStatusVariantsFromLLM() throws Exception {
        // Arrange — every field uses a non-canonical status (common small-model failure modes)
        String llmJson = """
                [
                  {"title": "Task A", "description": "Desc A", "status": "TO DO"},
                  {"title": "Task B", "description": "Desc B", "status": "In Progress"},
                  {"title": "Task C", "description": "Desc C", "status": "complete"},
                  {"title": "Task D", "description": "Desc D", "status": "BLOCKED"}
                ]
                """;
        when(callSpec.content()).thenReturn(llmJson);
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(
                Tasks.builder().taskId(1L).build(), Tasks.builder().taskId(2L).build(),
                Tasks.builder().taskId(3L).build(), Tasks.builder().taskId(4L).build()
        ));

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert — all statuses must be normalised to the four canonical values
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tasks>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());

        List<Tasks> saved = captor.getValue();
        assertThat(saved.get(0).getStatus()).isEqualTo("TODO");        // "TO DO"       → TODO
        assertThat(saved.get(1).getStatus()).isEqualTo("IN_PROGRESS"); // "In Progress" → IN_PROGRESS
        assertThat(saved.get(2).getStatus()).isEqualTo("DONE");        // "complete"    → DONE
        assertThat(saved.get(3).getStatus()).isEqualTo("BLOCKED");     // "BLOCKED"     → BLOCKED
    }

    // -------------------------------------------------------------------------
    // Format-specific: XLSX (Excel)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 6 - handles Excel (.xlsx) tabular output from TikaDocumentReader")
    void importTasksHandlesExcelSpreadsheetTabularOutput() throws Exception {
        // Arrange — simulate what TikaDocumentReader extracts from backlog-tasks.xlsx:
        // Tika renders spreadsheet rows as tab/space-separated lines.
        doReturn("Title\tDescription\tStatus\n"
                + "Implement login page\tBuild JWT-secured login form\tIN_PROGRESS\n"
                + "Write unit tests\tAchieve 90% JaCoCo coverage\tTODO\n"
                + "Deploy to K8s\tHelm chart deployment to prod\tBLOCKED")
                .when(spyService).extractText(any(MultipartFile.class));

        when(multipartFile.getOriginalFilename()).thenReturn("backlog-tasks.xlsx");
        when(multipartFile.getContentType()).thenReturn(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        String llmJson = """
                [
                  {"title":"Implement login page","description":"Build JWT-secured login form","status":"IN_PROGRESS"},
                  {"title":"Write unit tests","description":"Achieve 90% JaCoCo coverage","status":"TODO"},
                  {"title":"Deploy to K8s","description":"Helm chart deployment to prod","status":"BLOCKED"}
                ]
                """;
        when(callSpec.content()).thenReturn(llmJson);
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(
                Tasks.builder().taskId(1L).build(),
                Tasks.builder().taskId(2L).build(),
                Tasks.builder().taskId(3L).build()
        ));

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert — all 3 Excel rows saved as tasks
        assertThat(ids).hasSize(3).containsExactly(1L, 2L, 3L);

        // The spreadsheet text must be injected verbatim inside <document> tags
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Implement login page")
                .contains("SPREADSHEET FORMAT AWARENESS")
                .contains("<document>")
                .contains("</document>");
    }

    // -------------------------------------------------------------------------
    // Format-specific: DOCX (Word)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 6 - handles Word (.docx) output from TikaDocumentReader")
    void importTasksHandlesWordDocumentOutput() throws Exception {
        // Arrange — simulate TikaDocumentReader output for sprint-tasks.docx
        doReturn("Sprint 14 Task List\n\n"
                + "Configure Redis Cache\nDue: this sprint\nStatus: TODO\n\n"
                + "Fix N+1 query in UserService\nPriority: High\nStatus: IN_PROGRESS")
                .when(spyService).extractText(any(MultipartFile.class));

        when(multipartFile.getOriginalFilename()).thenReturn("sprint-tasks.docx");
        when(multipartFile.getContentType()).thenReturn(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        String llmJson = """
                [
                  {"title":"Configure Redis Cache","description":"Due this sprint","status":"TODO"},
                  {"title":"Fix N+1 query in UserService","description":"Priority: High","status":"IN_PROGRESS"}
                ]
                """;
        when(callSpec.content()).thenReturn(llmJson);
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(
                Tasks.builder().taskId(5L).build(),
                Tasks.builder().taskId(6L).build()
        ));

        // Act
        List<Long> ids = spyService.importTasksFromDocument(multipartFile);

        // Assert — both Word document tasks saved correctly
        assertThat(ids).hasSize(2).containsExactly(5L, 6L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tasks>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getTitle()).isEqualTo("Configure Redis Cache");
        assertThat(captor.getValue().get(1).getTitle()).isEqualTo("Fix N+1 query in UserService");
    }

    // -------------------------------------------------------------------------
    // stripLeadingTags — code-level title / description cleanup
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Feature 6 - stripLeadingTags removes <Task N> prefix from titles and <Acceptance Criteria> from descriptions")
    void importTasksStripsStructuralMarkerTagsFromTitlesAndDescriptions() throws Exception {
        // Arrange — mirrors the exact production output seen in the PDF import debug log:
        //   title:       "<Task N> Real task name"     (marker + real value)
        //   description: "<Acceptance Criteria> ..."   (sub-section label + real value)
        //   status:      "To Do" / "In Progress" / "Blocked"  (mixed-case)
        String llmJson = """
                [
                  {"title": "<Task 1> Build RESTful Product Catalogue API",
                   "description": "Implement Spring Boot REST endpoints for product catalogue module",
                   "status": "To Do"},
                  {"title": "<Task 2> Real-Time Inventory Tracking via WebSocket",
                   "description": "<Acceptance Criteria> Stock updates pushed over WebSocket and STOMP",
                   "status": "In Progress"},
                  {"title": "<Task 3> Migrate Session Auth to OAuth 2.0 with OIDC",
                   "description": "Replace custom session-based authentication with OAuth 2.0 and OIDC",
                   "status": "Blocked"},
                  {"title": "<Task 4> Integrate Elasticsearch for Full-Text Product Search",
                   "description": "Add Elasticsearch for full-text search with autocomplete suggestions",
                   "status": "To Do"},
                  {"title": "<Task 5> Build React Admin Analytics Dashboard",
                   "description": "Create admin dashboard in React using recharts",
                   "status": "To Do"}
                ]
                """;
        when(callSpec.content()).thenReturn(llmJson);
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(
                Tasks.builder().taskId(1L).build(), Tasks.builder().taskId(2L).build(),
                Tasks.builder().taskId(3L).build(), Tasks.builder().taskId(4L).build(),
                Tasks.builder().taskId(5L).build()
        ));

        // Act
        spyService.importTasksFromDocument(multipartFile);

        // Assert — <Task N> prefix must be stripped; only the real title is stored
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tasks>> captor = ArgumentCaptor.forClass(List.class);
        verify(taskRepository).saveAll(captor.capture());
        List<Tasks> saved = captor.getValue();

        assertThat(saved.get(0).getTitle()).isEqualTo("Build RESTful Product Catalogue API");
        assertThat(saved.get(1).getTitle()).isEqualTo("Real-Time Inventory Tracking via WebSocket");
        assertThat(saved.get(2).getTitle()).isEqualTo("Migrate Session Auth to OAuth 2.0 with OIDC");
        assertThat(saved.get(3).getTitle()).isEqualTo("Integrate Elasticsearch for Full-Text Product Search");
        assertThat(saved.get(4).getTitle()).isEqualTo("Build React Admin Analytics Dashboard");

        // <Acceptance Criteria> label must be stripped; only the content text is stored
        assertThat(saved.get(1).getDescription())
                .isEqualTo("Stock updates pushed over WebSocket and STOMP")
                .doesNotContain("<Acceptance Criteria>");

        // Status must be normalised to canonical values regardless of LLM casing
        assertThat(saved.get(0).getStatus()).isEqualTo("TODO");        // "To Do"      → TODO
        assertThat(saved.get(1).getStatus()).isEqualTo("IN_PROGRESS"); // "In Progress" → IN_PROGRESS
        assertThat(saved.get(2).getStatus()).isEqualTo("BLOCKED");     // "Blocked"     → BLOCKED
        assertThat(saved.get(3).getStatus()).isEqualTo("TODO");
        assertThat(saved.get(4).getStatus()).isEqualTo("TODO");
    }
}

