package fi.publishertools.kss.dto;

import java.util.Map;

/**
 * Response DTO for status endpoint.
 */
public class StatusResponse {

    private final String status;
    private final Map<String, Object> payload;
    private final String errorMessage;

    public StatusResponse(String status, Map<String, Object> payload) {
        this(status, payload, null);
    }

    public StatusResponse(String status, Map<String, Object> payload, String errorMessage) {
        this.status = status;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
