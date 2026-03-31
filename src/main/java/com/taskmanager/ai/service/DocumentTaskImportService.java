package com.taskmanager.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.api.dto.SaveTaskRequest;
import com.taskmanager.domain.model.Tasks;
import com.taskmanager.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Feature 6: Document Ingestion — PDF / Word → Tasks.
 *
 * <p>Architecture (no pgvector needed — regular relational storage):
 * <pre>
 * Upload PDF/Word
 *      │
 *      ▼
 * Spring AI DocumentReader   ← PagePdfDocumentReader / TikaDocumentReader
 *      │  (extracts plain text)
 *      ▼
 * Ollama Llama 3.2:1b        ← Structured JSON extraction prompt
 *      │  (returns JSON task list)
 *      ▼
 * Jackson JSON Parser        ← Deserialise into List&lt;SaveTaskRequest&gt;
 *      │
 *      ▼
 * TaskRepository.saveAll()   ← Regular PostgreSQL
 *      │
 *      ▼
 * Return saved task IDs
 * </pre>
 *
 * <p>Supported formats:
 * <ul>
 *   <li>PDF  — {@link PagePdfDocumentReader} (Spring AI native, no Tika overhead)</li>
 *   <li>Word (.docx/.doc), Excel, plain text — {@link TikaDocumentReader} (Apache Tika)</li>
 * </ul>
 *
 * <p>The {@link #extractText} method is {@code protected} so unit tests can stub it
 * via {@code Mockito.spy()} without needing actual document bytes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTaskImportService {

    private final ChatClient chatClient;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    /**
     * Extracts tasks from an uploaded PDF or Word document and persists them to PostgreSQL.
     *
     * @param file uploaded PDF or Word (.docx) file
     * @return list of database IDs of the saved tasks
     * @throws Exception if document reading or JSON parsing fails
     */
    @Transactional
    public List<Long> importTasksFromDocument(MultipartFile file) throws Exception {
        log.info("Importing tasks from document: {} ({})",
                file.getOriginalFilename(), file.getContentType());

        // Step 1 — Extract plain text from the uploaded document
        String rawText = extractText(file);
        log.debug("Extracted {} characters from document", rawText.length());

        // Step 2 — Ask Ollama to extract tasks as structured JSON
        //
        // Prompt-engineering notes for Llama 3.2 1B (small model):
        //   • System prompt primes the model to never copy examples
        //   • Document text is wrapped in <document> tags so the model treats it as DATA
        //   • DOCUMENT STRUCTURE AWARENESS section handles documents that use XML-like
        //     structural markers (e.g. <Task 1>, <Acceptance Criteria>) so the model
        //     picks the CONTENT that follows those markers, not the marker tags themselves
        //   • CRITICAL RULES section reinforces the "extract only, do not invent" constraint
        String userPrompt = """
                Extract ALL tasks, action items, and to-do items from the document inside the <document> tags.

                DOCUMENT STRUCTURE AWARENESS:
                The document may use structural markers such as <Task 1>, <Task 2> … <Task N> as section
                headers, and <Acceptance Criteria> (or similar labels) as sub-section separators.
                When you encounter this pattern:
                - The REAL task title is the TEXT that appears IMMEDIATELY AFTER the <Task N> marker line,
                  NOT the marker itself.  Never output "<Task 1>" as a title value.
                - The REAL description is the TEXT that appears AFTER the <Acceptance Criteria> label.
                  Do NOT include the label text "<Acceptance Criteria>" in the description field.
                - If a status value is present in the document, map it to the nearest allowed constant:
                  "to do" / "to-do" / "TO DO" → TODO,  "in progress" → IN_PROGRESS,  "done" / "complete" → DONE.

                SPREADSHEET FORMAT AWARENESS:
                If the document is a spreadsheet (Excel/XLSX), it may appear as tab-separated or
                space-aligned columns. The FIRST row is the header row — use it to identify which
                column contains the title, description, and status. Each SUBSEQUENT row is one task.
                Example header row:  Title  |  Description  |  Status
                Each data row below it maps directly to one JSON object in the output array.

                For every task you find, output these three fields:
                - "title"       : short title taken DIRECTLY from the document content (max 100 characters).
                                  Use the actual task name text, never a section-marker tag.
                - "description" : full detail of what needs to be done, taken DIRECTLY from the document.
                                  Omit structural labels (e.g., "<Acceptance Criteria>"); keep only the content.
                - "status"      : MUST be one of these EXACT uppercase strings only → TODO | IN_PROGRESS | DONE | BLOCKED
                                  Do NOT write "To Do", "In Progress", "Complete", or any other variation.
                                  Use TODO for not-started, IN_PROGRESS for active, DONE for finished, BLOCKED for blocked.

                CRITICAL RULES:
                1. Extract ONLY from the document text — do NOT invent, hallucinate, or add any tasks not present
                2. Do NOT copy or reuse anything from this instruction — use ONLY the content inside <document>
                3. Return ONE single JSON array containing ALL tasks — do NOT return multiple separate arrays
                4. Return ONLY the raw JSON array — no explanation, no markdown, no text before or after
                5. If no tasks are found, return exactly: []

                Required output format (use real values from the document — never copy this line):
                [{"title":"<title from doc>","description":"<description from doc>","status":"<STATUS>"}]

                <document>
                %s
                </document>
                """.formatted(rawText);

        String jsonResponse = chatClient.prompt()
                .system("You are a task extraction API. You output ONLY a raw JSON array of tasks "
                        + "extracted from the user-provided document. "
                        + "You NEVER copy examples or instructions into your output. "
                        + "You NEVER add explanation text. You output ONLY the JSON array.")
                .user(userPrompt)
                .call()
                .content();
        log.debug("LLM extracted tasks JSON: {}", jsonResponse);

        // Step 3 — Safely extract the JSON array (strips any narrative prose the model adds)
        String jsonArray = extractJsonArray(jsonResponse);

        // Step 4 — Deserialise JSON into SaveTaskRequest objects
        List<SaveTaskRequest> extractedTasks = objectMapper.readValue(
                jsonArray, new TypeReference<>() {});
        log.info("LLM extracted {} tasks from document", extractedTasks.size());

        // Step 5 — Persist each extracted task to PostgreSQL
        //   Three code-level safety nets run here, independent of prompt compliance:
        //   • stripLeadingTags(title)       — removes "<Task 1>" / "<Task N>" prefix the LLM
        //                                     often prepends to the real title value
        //   • stripLeadingTags(description) — removes "<Acceptance Criteria>" and similar
        //                                     sub-section labels the LLM includes verbatim
        //   • normalizeStatus(status)       — maps "To Do", "In Progress", "complete", etc.
        //                                     to the four canonical values
        List<Tasks> tasksToSave = extractedTasks.stream()
                .map(req -> Tasks.builder()
                        .title(stripLeadingTags(req.getTitle()))
                        .description(stripLeadingTags(req.getDescription()))
                        .status(normalizeStatus(req.getStatus()))
                        .build())
                .toList();

        List<Tasks> saved = taskRepository.saveAll(tasksToSave);
        List<Long> savedIds = saved.stream().map(Tasks::getTaskId).toList();
        log.info("Saved {} tasks from document. IDs: {}", savedIds.size(), savedIds);
        return savedIds;
    }

    /**
     * Extracts plain text from a PDF or Word document using Spring AI document readers.
     *
     * <p>Protected (not private) so that unit tests can stub this via
     * {@code Mockito.spy()} without requiring actual document bytes.
     *
     * @param file the uploaded multipart file
     * @return concatenated plain text of all document pages/sections
     * @throws Exception if the document cannot be read
     */
    protected String extractText(MultipartFile file) throws Exception {
        Resource resource  = file.getResource();
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String filename    = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        List<Document> documents;

        // Route to the right reader using BOTH content-type AND file extension.
        // Extension fallback handles cases where the browser/client sends a wrong or
        // generic content-type (e.g. "application/octet-stream") for a .pdf file.
        boolean isPdf = contentType.contains("pdf") || filename.endsWith(".pdf");

        if (isPdf) {
            // PagePdfDocumentReader — Spring AI native, no Tika overhead
            log.debug("Using PagePdfDocumentReader for '{}' (contentType: {})",
                    file.getOriginalFilename(), contentType);
            documents = new PagePdfDocumentReader(resource).get();
        } else {
            // TikaDocumentReader — handles .docx, .doc, .xlsx, .xls, .txt, and many more
            log.debug("Using TikaDocumentReader for '{}' (contentType: {})",
                    file.getOriginalFilename(), contentType);
            documents = new TikaDocumentReader(resource).get();
        }

        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Extracts and merges all JSON arrays from the LLM response into a single flat array.
     *
     * <p>Small models like Llama 3.2 1B have two failure modes this method handles:
     * <ol>
     *   <li><b>Narrative wrapper</b> — prose before/after the array is stripped by taking
     *       the substring from the first {@code [} to the last {@code ]}.</li>
     *   <li><b>One-array-per-task pattern</b> — the model returns N separate single-element
     *       arrays instead of one unified array:
     *       <pre>[{"title":"A"}]{@literal \n}[{"title":"B"}]</pre>
     *       This is fixed by replacing every {@code ][} (with optional whitespace) with {@code ,},
     *       producing a single valid JSON array that Jackson can deserialise in full.</li>
     * </ol>
     *
     * @param response raw LLM output
     * @return a single JSON array string, or {@code "[]"} if none found
     */
    private String extractJsonArray(String response) {
        if (response == null || response.isBlank()) {
            log.warn("LLM returned blank response for document import — using empty array");
            return "[]";
        }

        int start = response.indexOf('[');
        int end   = response.lastIndexOf(']');
        if (start < 0 || end <= start) {
            log.warn("No JSON array found in LLM response — returning empty array. Raw: {}", response);
            return "[]";
        }

        String extracted = response.substring(start, end + 1).trim();

        // Fix: merge the "one-array-per-task" pattern into a single flat array.
        // e.g.  [{"title":"A"}]\n[{"title":"B"}]  →  [{"title":"A"},{"title":"B"}]
        String merged = extracted.replaceAll("]\\s*\\[", ",");

        if (!merged.equals(extracted)) {
            log.debug("Merged multiple separate JSON arrays into one ({} chars)", merged.length());
        } else {
            log.debug("Extracted JSON array from LLM response ({} chars)", extracted.length());
        }
        return merged;
    }

    /**
     * Strips one or more leading structural marker tags from a string.
     *
     * <p>Llama 3.2 1B (and similar small models) frequently prepend the document's
     * XML-like section markers to the extracted field value, even after explicit
     * prompt instructions to omit them. This is the code-level counterpart to the
     * prompt's {@code DOCUMENT STRUCTURE AWARENESS} section.
     *
     * <p>The regex {@code ^(\s*<[^>]+>\s*)+} matches one or more consecutive
     * {@code <Tag>} blocks at the very start of the string, covering patterns like:
     * <ul>
     *   <li>{@code "<Task 1> Build RESTful API"} → {@code "Build RESTful API"}</li>
     *   <li>{@code "<Task 2>Real-Time Tracking"}  → {@code "Real-Time Tracking"}</li>
     *   <li>{@code "<Acceptance Criteria> Must pass CI"} → {@code "Must pass CI"}</li>
     *   <li>{@code "No marker here"}               → {@code "No marker here"} (unchanged)</li>
     * </ul>
     *
     * @param raw raw string value from LLM JSON (title or description)
     * @return cleaned string with all leading {@code <Tag>} prefixes stripped;
     *         if stripping would produce an empty string, the trimmed original is returned
     */
    private String stripLeadingTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        // Remove one or more consecutive leading <Tag> blocks, e.g. "<Task 1> ", "<Task 2>"
        String cleaned = raw.replaceAll("^(\\s*<[^>]+>\\s*)+", "").trim();
        // Safety: if everything was a tag (edge case), keep the trimmed original
        return cleaned.isBlank() ? raw.trim() : cleaned;
    }

    /**
     * Maps any status string the LLM might return to one of the four canonical values:
     * {@code TODO}, {@code IN_PROGRESS}, {@code DONE}, or {@code BLOCKED}.
     *
     * <p>Small models frequently ignore the prompt and return variants such as
     * {@code "TO DO"}, {@code "to-do"}, {@code "In Progress"}, {@code "Complete"}.
     * This method normalises all of them so the entity always stores a valid enum-like value.
     *
     * <p>Algorithm: strip all whitespace, hyphens, and underscores then upper-case, then match.
     * Examples:
     * <pre>
     *  "TO DO"       → "TODO"       (collapse space)
     *  "to-do"       → "TODO"       (collapse hyphen)
     *  "IN_PROGRESS" → "IN_PROGRESS"
     *  "In Progress" → "IN_PROGRESS"
     *  "complete"    → "DONE"
     *  "BLOCKED"     → "BLOCKED"
     * </pre>
     *
     * @param raw raw status string from LLM JSON
     * @return canonical status value
     */
    private String normalizeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "TODO";
        }
        // Remove all whitespace, hyphens, and underscores, then upper-case
        String key = raw.trim().toUpperCase().replaceAll("[\\s\\-_]+", "");
        return switch (key) {
            case "INPROGRESS"             -> "IN_PROGRESS";
            case "DONE", "COMPLETE",
                 "COMPLETED", "FINISHED"  -> "DONE";
            case "BLOCKED"                -> "BLOCKED";
            default                       -> "TODO";   // covers "TODO", "TOTO", "NOTSTARTED", etc.
        };
    }
}

