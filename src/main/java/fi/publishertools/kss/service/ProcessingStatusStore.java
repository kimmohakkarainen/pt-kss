package fi.publishertools.kss.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import fi.publishertools.kss.model.ProcessingStatus;

/**
 * Thread-safe store for tracking processing status per file ID.
 */
@Component
public class ProcessingStatusStore {

    private final Map<String, ProcessingStatus> statusMap = new ConcurrentHashMap<>();

    public void setStatus(String fileId, ProcessingStatus status) {
        statusMap.put(fileId, status);
    }

    public Optional<ProcessingStatus> getStatus(String fileId) {
        return Optional.ofNullable(statusMap.get(fileId));
    }

    public ProcessingStatus getStatusOrDefault(String fileId, ProcessingStatus defaultStatus) {
        return statusMap.getOrDefault(fileId, defaultStatus);
    }

    public void remove(String fileId) {
        statusMap.remove(fileId);
    }
}
