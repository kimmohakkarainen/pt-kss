package fi.publishertools.kss.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import fi.publishertools.kss.model.ProcessedResult;
import fi.publishertools.kss.model.ProcessingStatus;

/**
 * Thread-safe store for final processed results.
 */
@Component
public class ProcessedResultStore {

    private final Map<String, ProcessedResult> resultMap = new ConcurrentHashMap<>();

    public void storeResult(String fileId, Map<String, Object> payload) {
        ProcessedResult result = new ProcessedResult(
                fileId,
                ProcessingStatus.READY,
                Instant.now(),
                payload
        );
        resultMap.put(fileId, result);
    }

    public void storeError(String fileId, String errorMessage) {
        ProcessedResult result = new ProcessedResult(
                fileId,
                ProcessingStatus.ERROR,
                Instant.now(),
                null,
                errorMessage
        );
        resultMap.put(fileId, result);
    }

    public Optional<ProcessedResult> getResult(String fileId) {
        return Optional.ofNullable(resultMap.get(fileId));
    }

    public void remove(String fileId) {
        resultMap.remove(fileId);
    }
}
