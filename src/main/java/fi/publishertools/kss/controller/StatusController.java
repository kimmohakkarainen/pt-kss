package fi.publishertools.kss.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.publishertools.kss.dto.StatusResponse;
import fi.publishertools.kss.model.ProcessedResult;
import fi.publishertools.kss.model.ProcessingStatus;
import fi.publishertools.kss.service.ProcessedResultStore;
import fi.publishertools.kss.service.ProcessingStatusStore;

/**
 * REST controller for checking processing status.
 */
@RestController
@RequestMapping("/api/v1")
public class StatusController {

    private final ProcessingStatusStore statusStore;
    private final ProcessedResultStore resultStore;

    public StatusController(ProcessingStatusStore statusStore, ProcessedResultStore resultStore) {
        this.statusStore = statusStore;
        this.resultStore = resultStore;
    }

    @GetMapping(
            path = "/status/{fileId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StatusResponse> getStatus(@PathVariable String fileId) {
        // Check if we have a final result (ready or error)
        ProcessedResult result = resultStore.getResult(fileId).orElse(null);

        if (result != null) {
            // We have a final result
            if (result.getStatus() == ProcessingStatus.READY) {
                return ResponseEntity.ok(new StatusResponse(
                        "ready",
                        result.getPayload(),
                        null
                ));
            } else if (result.getStatus() == ProcessingStatus.ERROR) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new StatusResponse(
                                "error",
                                null,
                                result.getErrorMessage()
                        ));
            }
        }

        // Check current status
        ProcessingStatus currentStatus = statusStore.getStatusOrDefault(fileId, ProcessingStatus.IN_PROGRESS);

        if (currentStatus == ProcessingStatus.AWAITING_METADATA) {
            return ResponseEntity.ok(new StatusResponse("awaiting-metadata", null, null));
        }
        if (currentStatus == ProcessingStatus.IN_PROGRESS) {
            return ResponseEntity.ok(new StatusResponse("in-progress", null, null));
        }

        // Fallback: return in-progress if status is unknown
        return ResponseEntity.ok(new StatusResponse("in-progress", null, null));
    }
}
