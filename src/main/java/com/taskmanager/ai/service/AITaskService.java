package com.taskmanager.ai.service;

import com.taskmanager.api.dto.GetTaskResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * AI Task Service — AI-powered features for the Task Management Application.
 *
 * <ul>
 *   <li><b>Feature 1</b> — Description Generator: Prompt Engineering, LLM text generation.</li>
 *   <li><b>Feature 2</b> — Status Suggester: Structured JSON output from LLM.</li>
 *   <li><b>Feature 3</b> — Task Summarizer: RAG pattern — DB data injected into LLM context.</li>
 *   <li><b>Feature 4</b> — Task Breakdown: Chain-of-thought prompting, structured list output.</li>
 * </ul>
 *
 * <p>Every LLM call is wrapped with a Micrometer {@link Timer} so AI response
 * latency is exported to Prometheus and visible in Grafana dashboards.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AITaskService {

    private final ChatClient chatClient;
    private final MeterRegistry meterRegistry;

    /**
     * Feature 1: Generate a professional task description from a title.
     *
     * <p>AI Pattern: <b>Prompt Engineering</b> — a carefully structured system
     * prompt instructs the LLM to produce a consistent, actionable description
     * covering what needs to be done, why it matters, and acceptance criteria.
     *
     * @param title the task title provided by the user
     * @return a 3-4 sentence professional task description
     */
    public String generateDescription(String title) {
        log.info("AI generating description for title: {}", title);

        Timer timer = meterRegistry.timer(
                "ai.task.generate_description",
                "model", "llama3.2:1b",
                "feature", "description_generator");

        return timer.record((Supplier<String>) () -> {
            String prompt = """
                    You are a senior project management assistant.
                    Generate a clear, concise task description (3-4 sentences) for a task titled: "%s"
                    Cover: what needs to be done, why it matters, and Acceptance Criteria.
                    Be professional and specific. Do not include the title in the response.
                    """.formatted(title);

            return chatClient.prompt().user(prompt).call().content();
        });
    }

    /**
     * Feature 2: Suggest the most appropriate task status from title and description.
     *
     * <p>AI Pattern: <b>Structured JSON Output</b> — the prompt constrains the LLM
     * to reply only with a JSON object containing {@code status} and {@code reason}.
     * Valid statuses: {@code TODO}, {@code IN_PROGRESS}, {@code DONE}, {@code BLOCKED}.
     *
     * <p>Example response:
     * <pre>{"status": "TODO", "reason": "No work has started on this feature yet."}</pre>
     *
     * @param title       the task title
     * @param description the task description
     * @return JSON string with suggested status and reasoning
     */
    public String suggestStatus(String title, String description) {
        log.info("AI suggesting status for title: {}", title);

        Timer timer = meterRegistry.timer(
                "ai.task.suggest_status",
                "model", "llama3.2:1b",
                "feature", "status_suggester");

        return timer.record((Supplier<String>) () -> {
            String prompt = """
                    Analyze this software task and pick the correct status.

                    STATUS RULES (pick exactly one):
                    - TODO       : No work started yet.
                    - IN_PROGRESS: Work is actively in progress by the developer.
                    - DONE       : Work is fully completed and verified.
                    - BLOCKED    : Cannot proceed. Use BLOCKED if ANY of these apply:
                                   * Developer says "not able to", "cannot proceed", "waiting for"
                                   * Internet, network, or environment is unavailable
                                   * Missing access, credentials, approval, or external dependency

                    Title: %s
                    Description: %s

                    Fill in STATUS and REASON and respond with ONLY this JSON, nothing else:
                    {"status": "<STATUS>", "reason": "<REASON>"}
                    """.formatted(title, description);

            String raw = chatClient.prompt()
                    .system("You are a JSON API. You ONLY output a single valid JSON object. "
                            + "Never write any text, explanation, or markdown before or after the JSON.")
                    .user(prompt)
                    .call()
                    .content();

            return extractJson(raw);
        });
    }

    /**
     * Feature 3: Summarize all tasks fetched from the database.
     *
     * <p>AI Pattern: <b>RAG (Retrieval-Augmented Generation)</b> — the LLM has no direct
     * access to PostgreSQL. Tasks are fetched first, then injected as plain text into
     * the prompt so the model reasons over <em>live application data</em> rather than
     * hallucinating. This is the most important AI pattern to demonstrate in an interview.
     *
     * <p>Returns an early fallback message when no tasks exist so the LLM is never
     * called unnecessarily.
     *
     * @param tasks list of all tasks retrieved from the database
     * @return a concise 5-point executive summary of the current project state
     */
    public String summarizeAllTasks(List<GetTaskResponse> tasks) {
        log.info("AI summarizing {} tasks", tasks.size());

        if (tasks.isEmpty()) {
            return "No tasks found in the system.";
        }

        Timer timer = meterRegistry.timer(
                "ai.task.summarize_all_tasks",
                "model", "llama3.2:1b",
                "feature", "task_summarizer");

        return timer.record((Supplier<String>) () -> {
            String taskList = tasks.stream()
                    .map(t -> "- [%s] %s: %s".formatted(t.getStatus(), t.getTitle(), t.getDescription()))
                    .collect(Collectors.joining("\n"));

            String prompt = """
                    You are a senior project manager. Analyze the following task list and provide:
                    1. Overall project health (1-2 sentences)
                    2. Completed work summary
                    3. Work in progress
                    4. Potential blockers or risks
                    5. Recommended next actions

                    Tasks:
                    %s

                    Be concise and professional.
                    """.formatted(taskList);

            return chatClient.prompt().user(prompt).call().content();
        });
    }

    /**
     * Feature 4: Break a complex task into actionable subtasks.
     *
     * <p>AI Pattern: <b>Chain-of-Thought Prompting</b> — the prompt instructs the model
     * to reason step-by-step, producing 5–7 specific subtasks each completable in 1–2 hours.
     * The output format is constrained to a numbered list so the response is immediately
     * usable in sprint planning and story-point estimation.
     *
     * @param title       the complex task title
     * @param description the full task description
     * @return a numbered list of 5–7 actionable subtasks as plain text
     */
    public String breakdownTask(String title, String description) {
        log.info("AI breaking down task: {}", title);

        Timer timer = meterRegistry.timer(
                "ai.task.breakdown",
                "model", "llama3.2:1b",
                "feature", "task_breakdown");

        return timer.record((Supplier<String>) () -> {
            String prompt = """
                    You are a senior software engineer and scrum master.
                    Break down this task into 5-7 specific, actionable subtasks.
                    Each subtask must be independently completable in 1-2 hours.
                    Each subtask should have a clear action verb and specific deliverable.

                    Task Title: %s
                    Task Description: %s

                    Format your response as a numbered list only. No introduction or conclusion text.
                    """.formatted(title, description);

            return chatClient.prompt().user(prompt).call().content();
        });
    }

    /**
     * Extracts the first valid JSON object from the LLM response.
     *
     * <p>Small models like Llama 3.2 1B sometimes wrap the JSON in narrative prose
     * (e.g. "Based on the task, I recommend... {"status":"BLOCKED",...}").
     * This method strips everything outside the outermost {@code { }} braces so the
     * caller always receives clean, parseable JSON.
     *
     * @param response raw LLM output
     * @return trimmed JSON object, or a fallback JSON string if no braces found
     */
    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            log.warn("LLM returned blank response for suggestStatus — using fallback JSON");
            return "{\"status\":\"TODO\",\"reason\":\"Unable to determine status from the provided description.\"}";
        }
        String trimmed = response.trim();
        int start = trimmed.indexOf('{');
        int end   = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String extracted = trimmed.substring(start, end + 1);
            log.debug("Extracted JSON from LLM response: {}", extracted);
            return extracted;
        }
        log.warn("LLM response contained no JSON object — returning raw response: {}", trimmed);
        return trimmed;
    }
}
