package fi.publishertools.kss.service;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import fi.publishertools.kss.UploadProperties;
import fi.publishertools.kss.model.FileTooLargeException;
import fi.publishertools.kss.model.StoredFile;

@Service
public class UploadService {

    private final UploadProperties uploadProperties;
    private final InMemoryFileStore fileStore;

    public UploadService(UploadProperties uploadProperties, InMemoryFileStore fileStore) {
        this.uploadProperties = uploadProperties;
        this.fileStore = fileStore;
    }

    public StoredFile storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        long maxSize = uploadProperties.getMaxSizeBytes();
        if (maxSize > 0 && file.getSize() > maxSize) {
            throw new FileTooLargeException("File exceeds maximum allowed size of " + maxSize + " bytes");
        }

        String id = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();
        Instant uploadTime = Instant.now();
        byte[] data = file.getBytes();

        StoredFile storedFile = new StoredFile(
                id,
                originalFilename,
                contentType,
                size,
                uploadTime,
                data
        );

        fileStore.save(storedFile);
        return storedFile;
    }
}

