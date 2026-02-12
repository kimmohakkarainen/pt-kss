package fi.publishertools.kss.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.publishertools.kss.dto.ErrorResponse;
import fi.publishertools.kss.dto.LangMarkupOccurrenceDetail;
import fi.publishertools.kss.dto.LangMarkupOccurrenceSummary;
import fi.publishertools.kss.dto.LangMarkupReviewSummary;
import fi.publishertools.kss.dto.LangMarkupUpdateRequest;
import fi.publishertools.kss.exception.PendingLangMarkupNotFoundException;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.service.LangMarkupReviewService;
import fi.publishertools.kss.service.PendingLangMarkupStore;
import fi.publishertools.kss.service.ProcessingPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Language markup review", description = "Review and edit proposed language markup (non-main-language phrases) before XHTML generation.")
public class LangMarkupReviewController {

    private final PendingLangMarkupStore pendingLangMarkupStore;
    private final ProcessingPipelineService pipelineService;
    private final LangMarkupReviewService langMarkupReviewService;

    public LangMarkupReviewController(PendingLangMarkupStore pendingLangMarkupStore,
                                      ProcessingPipelineService pipelineService,
                                      LangMarkupReviewService langMarkupReviewService) {
        this.pendingLangMarkupStore = pendingLangMarkupStore;
        this.pipelineService = pipelineService;
        this.langMarkupReviewService = langMarkupReviewService;
    }

    @Operation(summary = "List lang markup review", description = "List all files currently in AWAITING_LANG_MARKUP_REVIEW phase (proposed language markup ready for user review).")
    @ApiResponse(responseCode = "200", description = "List of files awaiting lang markup review (fileId, originalFilename)")
    @GetMapping(
            path = "/lang-markup-review",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<LangMarkupReviewSummary>> listPending() {
        List<LangMarkupReviewSummary> summaries = pendingLangMarkupStore.listFileIds().stream()
                .map(fileId -> pendingLangMarkupStore.get(fileId)
                        .map(ctx -> new LangMarkupReviewSummary(ctx.getFileId(), ctx.getOriginalFilename()))
                        .orElse(null))
                .filter(s -> s != null)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @Operation(summary = "List language markup occurrences", description = "List language markup occurrences in document order for a file awaiting review. Each item has index, phraseText, and proposedLanguage.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of language markup occurrences (index 0-based, in document order)"),
            @ApiResponse(responseCode = "404", description = "Lang markup review not found for file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(
            path = "/lang-markup-review/{fileId}/occurrences",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<LangMarkupOccurrenceSummary>> listOccurrences(@PathVariable String fileId) {
        ProcessingContext context = getContextOrThrow(fileId);
        List<LangMarkupOccurrenceSummary> list = langMarkupReviewService.getOccurrenceSummaries(context.getChapters());
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get occurrence detail", description = "Get detail for one language markup occurrence: phraseText, proposedLanguage, textBefore, textAfter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Occurrence detail with textBefore and textAfter"),
            @ApiResponse(responseCode = "400", description = "Invalid occurrence index", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lang markup review or occurrence not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(
            path = "/lang-markup-review/{fileId}/occurrences/{index}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LangMarkupOccurrenceDetail> getOccurrence(@PathVariable String fileId, @PathVariable int index) {
        ProcessingContext context = getContextOrThrow(fileId);
        requireValidIndex(context, index);
        LangMarkupOccurrenceDetail detail = langMarkupReviewService.getOccurrenceDetail(context.getChapters(), index);
        if (detail == null) {
            throw new PendingLangMarkupNotFoundException("Occurrence not found for index: " + index);
        }
        return ResponseEntity.ok(detail);
    }

    @Operation(summary = "Update occurrence", description = "Update language markup for an occurrence. Request body: optional language, phraseText, textBefore, textAfter; or dismiss (boolean) to merge segment back. If dismiss is true, other fields are ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated occurrence summary"),
            @ApiResponse(responseCode = "400", description = "Invalid occurrence index", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Lang markup review or occurrence not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping(
            path = "/lang-markup-review/{fileId}/occurrences/{index}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LangMarkupOccurrenceSummary> updateOccurrence(@PathVariable String fileId,
                                                                        @PathVariable int index,
                                                                        @RequestBody(required = false) LangMarkupUpdateRequest request) {
        ProcessingContext context = getContextOrThrow(fileId);
        requireValidIndex(context, index);
        LangMarkupUpdateRequest req = request != null ? request : new LangMarkupUpdateRequest();
        LangMarkupOccurrenceSummary summary = langMarkupReviewService.applyUpdate(context, index, req);
        if (summary == null) {
            throw new PendingLangMarkupNotFoundException("Occurrence not found for index: " + index);
        }
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Approve lang markup review", description = "Signal that no more editing is needed. Context is removed from the store and processing continues from XHTML generation (C1) to EPUB assembly.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Approval accepted, processing resumed"),
            @ApiResponse(responseCode = "404", description = "Lang markup review not found for file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(
            path = "/lang-markup-review/{fileId}/approve",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> approve(@PathVariable String fileId) {
        ProcessingContext context = pendingLangMarkupStore.remove(fileId)
                .orElseThrow(() -> new PendingLangMarkupNotFoundException("Lang markup review not found for file: " + fileId));
        pipelineService.resubmitAfterLangMarkupReview(context);
        return ResponseEntity.accepted().build();
    }

    private ProcessingContext getContextOrThrow(String fileId) {
        return pendingLangMarkupStore.get(fileId)
                .orElseThrow(() -> new PendingLangMarkupNotFoundException("Lang markup review not found for file: " + fileId));
    }

    private void requireValidIndex(ProcessingContext context, int index) {
        if (!langMarkupReviewService.isValidOccurrenceIndex(context, index)) {
            throw new IllegalArgumentException("Invalid occurrence index: " + index);
        }
    }
}
