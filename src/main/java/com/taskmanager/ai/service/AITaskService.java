package com.taskmanager.ai.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * AI Task Service — AI-powered features for the Task Management Application.
 *
 * <ul>
 *   <li><b>Feature 1</b> — Description Generator: Prompt Engineering, LLM text generation.</li>
 *   <li><b>Feature 2</b> — Status Suggester: Structured JSON output from LLM.</li>
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

            return chatClient.prompt()
                    .system("You are a JSON API. You ONLY output a single valid JSON object. "
                            + "Never write any text, explanation, or markdown before or after the JSON.")
                    .user(prompt)
                    .call()
                    .content();
        });
    }
}
