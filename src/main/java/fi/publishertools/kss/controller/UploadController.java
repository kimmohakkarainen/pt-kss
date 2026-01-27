package fi.publishertools.kss.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fi.publishertools.kss.dto.UploadResponse;
import fi.publishertools.kss.model.InvalidContentTypeException;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.service.UploadService;

@RestController
@RequestMapping("/api/v1")
public class UploadController {

    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null ||
                (!ZIP_CONTENT_TYPE.equalsIgnoreCase(contentType)
                        && !OCTET_STREAM_CONTENT_TYPE.equalsIgnoreCase(contentType))) {
            throw new InvalidContentTypeException("Only application/zip or application/octet-stream are allowed");
        }

        StoredFile storedFile = uploadService.storeFile(file);

        UploadResponse response = new UploadResponse(
                storedFile.getId(),
                storedFile.getOriginalFilename(),
                storedFile.getContentType(),
                storedFile.getSize(),
                storedFile.getUploadTime()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

