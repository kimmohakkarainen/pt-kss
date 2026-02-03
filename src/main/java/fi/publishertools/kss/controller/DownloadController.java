package fi.publishertools.kss.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.publishertools.kss.dto.ErrorResponse;
import fi.publishertools.kss.model.DownloadableFile;
import fi.publishertools.kss.service.EpubDownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST controller for downloading ready-made EPUB files by upload/processing ID.
 */
@RestController
@RequestMapping("/api/v1")
public class DownloadController {

    private static final String EPUB_MEDIA_TYPE = "application/epub+zip";

    private final EpubDownloadService epubDownloadService;

    public DownloadController(EpubDownloadService epubDownloadService) {
        this.epubDownloadService = epubDownloadService;
    }

    @Operation(summary = "Download EPUB", description = "Download a ready-made EPUB file by upload/processing ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EPUB file"),
            @ApiResponse(responseCode = "404", description = "EPUB not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(
            path = "/epub/{id}",
            produces = EPUB_MEDIA_TYPE
    )
    public ResponseEntity<ByteArrayResource> downloadEpub(@PathVariable("id") String id) {
        DownloadableFile file = epubDownloadService.getEpub(id);

        ByteArrayResource resource = new ByteArrayResource(file.getContent());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentLength(file.getContentLength())
                .body(resource);
    }
}
