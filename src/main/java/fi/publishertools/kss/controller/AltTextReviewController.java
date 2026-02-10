package fi.publishertools.kss.controller;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.publishertools.kss.dto.AltTextOccurrenceDetail;
import fi.publishertools.kss.dto.AltTextOccurrenceSummary;
import fi.publishertools.kss.dto.AltTextReviewSummary;
import fi.publishertools.kss.dto.AltTextUpdateRequest;
import fi.publishertools.kss.dto.ErrorResponse;
import fi.publishertools.kss.exception.PendingAltTextNotFoundException;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.phases.A3_ExtractImageInfo;
import fi.publishertools.kss.service.AltTextReviewService;
import fi.publishertools.kss.service.PendingAltTextStore;
import fi.publishertools.kss.service.ProcessingPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1")
public class AltTextReviewController {

    private final PendingAltTextStore pendingAltTextStore;
    private final ProcessingPipelineService pipelineService;
    private final AltTextReviewService altTextReviewService;

    public AltTextReviewController(PendingAltTextStore pendingAltTextStore,
                                   ProcessingPipelineService pipelineService,
                                   AltTextReviewService altTextReviewService) {
        this.pendingAltTextStore = pendingAltTextStore;
        this.pipelineService = pipelineService;
        this.altTextReviewService = altTextReviewService;
    }

    @Operation(summary = "List alt text review", description = "List all files currently in AWAITING_ALT_TEXTS phase (proposed alt texts ready for user review).")
    @ApiResponse(responseCode = "200", description = "List of files awaiting alt text review (fileId, originalFilename)")
    @GetMapping(
            path = "/alt-text-review",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<AltTextReviewSummary>> listPending() {
        List<AltTextReviewSummary> summaries = pendingAltTextStore.listFileIds().stream()
                .map(fileId -> pendingAltTextStore.get(fileId)
                        .map(ctx -> new AltTextReviewSummary(ctx.getFileId(), ctx.getOriginalFilename()))
                        .orElse(null))
                .filter(s -> s != null)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @Operation(summary = "List image occurrences", description = "List image occurrences in document order for a file awaiting alt text review. Each item has index, fileName, proposedAltText, and alternateText (current value).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of image occurrences (index 0-based, in document order)"),
            @ApiResponse(responseCode = "404", description = "Alt text review not found for file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(
            path = "/alt-text-review/{fileId}/occurrences",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<AltTextOccurrenceSummary>> listOccurrences(@PathVariable String fileId) {
        ProcessingContext context = getContextOrThrow(fileId);
        List<ImageNode> imageList = context.getImageList();
        if (imageList == null || imageList.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<AltTextOccurrenceSummary> list = IntStream.range(0, imageList.size())
                .mapToObj(i -> {
                    ImageNode node = imageList.get(i);
                    return new AltTextOccurrenceSummary(
                            i,
                            node.fileName() != null ? node.fileName() : "",
                            node.alternateText() != null ? node.alternateText() : "",
                            node.alternateText() != null ? node.alternateText() : ""
                    );
                })
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Get occurrence detail", description = "Get detail for one image occurrence: image ref, proposed/current alt text, and surrounding document text (textBefore, textAfter).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Occurrence detail with textBefore and textAfter"),
            @ApiResponse(responseCode = "400", description = "Invalid occurrence index", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alt text review or occurrence not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(
            path = "/alt-text-review/{fileId}/occurrences/{index}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AltTextOccurrenceDetail> getOccurrence(@PathVariable String fileId, @PathVariable int index) {
        ProcessingContext context = getContextOrThrow(fileId);
        requireValidIndex(context, index);
        List<ImageNode> imageList = context.getImageList();
        ImageNode node = imageList.get(index);
        String proposed = node.alternateText() != null ? node.alternateText() : "";
        AltTextReviewService.SurroundingText surrounding = altTextReviewService.getSurroundingText(context.getChapters(), index);
        AltTextOccurrenceDetail detail = new AltTextOccurrenceDetail(
                index,
                node.fileName() != null ? node.fileName() : "",
                proposed,
                proposed,
                surrounding.textBefore(),
                surrounding.textAfter()
        );
        return ResponseEntity.ok(detail);
    }

    @Operation(summary = "Get occurrence image", description = "Serve image bytes for an occurrence. Content-Type is set from the image filename (e.g. image/jpeg, image/png).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image bytes with appropriate Content-Type"),
            @ApiResponse(responseCode = "400", description = "Invalid occurrence index", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alt text review or occurrence or image content not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(
            path = "/alt-text-review/{fileId}/occurrences/{index}/image",
            produces = { MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/gif", "image/svg+xml", "image/webp", MediaType.APPLICATION_OCTET_STREAM_VALUE }
    )
    public ResponseEntity<byte[]> getOccurrenceImage(@PathVariable String fileId, @PathVariable int index) {
        ProcessingContext context = getContextOrThrow(fileId);
        requireValidIndex(context, index);
        List<ImageNode> imageList = context.getImageList();
        String fileName = imageList.get(index).fileName();
        byte[] content = context.getImageContent(fileName);
        if (content == null || content.length == 0) {
            throw new PendingAltTextNotFoundException("Image content not found for occurrence " + index);
        }
        String contentType = A3_ExtractImageInfo.getMimeTypeFromFilename(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(summary = "Update occurrence alt text", description = "Save edited alt text for an image occurrence. Request body: { \"alternateText\": \"...\" }. Empty string clears the alt text.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated occurrence summary"),
            @ApiResponse(responseCode = "400", description = "Invalid occurrence index", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alt text review or occurrence not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping(
            path = "/alt-text-review/{fileId}/occurrences/{index}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AltTextOccurrenceSummary> updateOccurrenceAltText(@PathVariable String fileId,
                                                                             @PathVariable int index,
                                                                             @RequestBody AltTextUpdateRequest request) {
        ProcessingContext context = getContextOrThrow(fileId);
        requireValidIndex(context, index);
        String text = request.getAlternateText() != null ? request.getAlternateText() : "";
        List<ImageNode> imageList = context.getImageList();
        imageList.get(index).setAlternateText(text);
        altTextReviewService.syncAltTextToTreeAt(context, index);
        ImageNode node = imageList.get(index);
        AltTextOccurrenceSummary summary = new AltTextOccurrenceSummary(
                index,
                node.fileName() != null ? node.fileName() : "",
                node.alternateText() != null ? node.alternateText() : "",
                node.alternateText() != null ? node.alternateText() : ""
        );
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Approve alt text review", description = "Signal that no more editing is needed. Context is removed from the store and processing continues from XHTML generation (C1) to EPUB assembly.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Approval accepted, processing resumed"),
            @ApiResponse(responseCode = "404", description = "Alt text review not found for file", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(
            path = "/alt-text-review/{fileId}/approve",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> approve(@PathVariable String fileId) {
        ProcessingContext context = pendingAltTextStore.remove(fileId)
                .orElseThrow(() -> new PendingAltTextNotFoundException("Alt text review not found for file: " + fileId));
        pipelineService.resubmitAfterAltTextReview(context);
        return ResponseEntity.accepted().build();
    }

    private ProcessingContext getContextOrThrow(String fileId) {
        return pendingAltTextStore.get(fileId)
                .orElseThrow(() -> new PendingAltTextNotFoundException("Alt text review not found for file: " + fileId));
    }

    private void requireValidIndex(ProcessingContext context, int index) {
        if (!altTextReviewService.isValidOccurrenceIndex(context, index)) {
            throw new IllegalArgumentException("Invalid occurrence index: " + index);
        }
    }
}
