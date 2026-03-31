package com.taskmanager.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO for Feature 6: Document Ingestion.
 *
 * <p>Returns a human-readable message and the IDs of every task
 * that was extracted from the uploaded document and persisted to PostgreSQL.
 */
@Getter
@Builder
public class ImportDocumentResponse {

    /** Summary message, e.g. "Successfully imported 5 tasks". */
    private String message;

    /** Database IDs of the tasks that were created from the document. */
    private List<Long> taskIds;
}

