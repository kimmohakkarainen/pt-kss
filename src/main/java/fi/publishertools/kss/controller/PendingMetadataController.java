package fi.publishertools.kss.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.publishertools.kss.dto.PendingMetadataResponse;
import fi.publishertools.kss.dto.PendingMetadataSummary;
import fi.publishertools.kss.dto.PendingMetadataUpdateRequest;
import fi.publishertools.kss.model.PendingMetadataNotFoundException;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.phases.CheckMandatoryInformationPhase;
import fi.publishertools.kss.service.PendingMetadataStore;
import fi.publishertools.kss.service.ProcessingPipelineService;

@RestController
@RequestMapping("/api/v1")
public class PendingMetadataController {

    private final PendingMetadataStore pendingMetadataStore;
    private final ProcessingPipelineService pipelineService;

    public PendingMetadataController(PendingMetadataStore pendingMetadataStore,
                                      ProcessingPipelineService pipelineService) {
        this.pendingMetadataStore = pendingMetadataStore;
        this.pipelineService = pipelineService;
    }

    @GetMapping(
            path = "/pending-metadata",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<PendingMetadataSummary>> listPending() {
        List<PendingMetadataSummary> summaries = pendingMetadataStore.listFileIds().stream()
                .map(fileId -> pendingMetadataStore.get(fileId)
                        .map(ctx -> new PendingMetadataSummary(ctx.getFileId(), ctx.getOriginalFilename()))
                        .orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    @GetMapping(
            path = "/pending-metadata/{fileId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PendingMetadataResponse> getPendingMetadata(@PathVariable String fileId) {
        ProcessingContext context = pendingMetadataStore.get(fileId)
                .orElseThrow(() -> new PendingMetadataNotFoundException("Pending metadata not found for file: " + fileId));

        Map<String, Object> metadataMap = buildMetadataMap(context);
        List<String> missingFields = CheckMandatoryInformationPhase.getMissingFields(context);

        return ResponseEntity.ok(new PendingMetadataResponse(
                context.getFileId(),
                context.getOriginalFilename(),
                metadataMap,
                missingFields
        ));
    }

    @PatchMapping(
            path = "/pending-metadata/{fileId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PendingMetadataResponse> updateMetadata(@PathVariable String fileId,
                                                                  @RequestBody PendingMetadataUpdateRequest request) {
        ProcessingContext context = pendingMetadataStore.get(fileId)
                .orElseThrow(() -> new PendingMetadataNotFoundException("Pending metadata not found for file: " + fileId));

        applyUpdates(context, request);

        Map<String, Object> metadataMap = buildMetadataMap(context);
        List<String> missingFields = CheckMandatoryInformationPhase.getMissingFields(context);

        return ResponseEntity.ok(new PendingMetadataResponse(
                context.getFileId(),
                context.getOriginalFilename(),
                metadataMap,
                missingFields
        ));
    }

    @PostMapping(
            path = "/pending-metadata/{fileId}/approve",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> approve(@PathVariable String fileId) {
        ProcessingContext context = pendingMetadataStore.remove(fileId)
                .orElseThrow(() -> new PendingMetadataNotFoundException("Pending metadata not found for file: " + fileId));

        pipelineService.resubmitForMandatoryCheck(context);
        return ResponseEntity.accepted().build();
    }

    private Map<String, Object> buildMetadataMap(ProcessingContext context) {
        Map<String, Object> result = new HashMap<>();
        for (String key : CheckMandatoryInformationPhase.getMandatoryKeys()) {
            String value = context.getMetadata(key, String.class);
            result.put(key, value);
        }
        return result;
    }

    private void applyUpdates(ProcessingContext context, PendingMetadataUpdateRequest request) {
        if (request.getTitle() != null) {
            context.addMetadata("title", request.getTitle());
        }
        if (request.getCreator() != null) {
            context.addMetadata("creator", request.getCreator());
        }
        if (request.getPublisher() != null) {
            context.addMetadata("publisher", request.getPublisher());
        }
        if (request.getLanguage() != null) {
            context.addMetadata("language", request.getLanguage());
        }
        if (request.getIdentifier() != null) {
            context.addMetadata("identifier", request.getIdentifier());
        }
    }
}
