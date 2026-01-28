package fi.publishertools.kss.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.publishertools.kss.model.DownloadableFile;
import fi.publishertools.kss.service.EpubDownloadService;

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
