package fi.publishertools.kss.model;

import java.time.Instant;
import java.util.Map;

/**
 * Final processed result stored after all phases complete.
 */
public class ProcessedResult {

    private final String fileId;
    private final ProcessingStatus status;
    private final Instant processedAt;
    private final Map<String, Object> payload;
    private final String errorMessage;

    public ProcessedResult(String fileId, ProcessingStatus status, Instant processedAt, Map<String, Object> payload) {
        this(fileId, status, processedAt, payload, null);
    }

    public ProcessedResult(String fileId, ProcessingStatus status, Instant processedAt, Map<String, Object> payload, String errorMessage) {
        this.fileId = fileId;
        this.status = status;
        this.processedAt = processedAt;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    public String getFileId() {
        return fileId;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
