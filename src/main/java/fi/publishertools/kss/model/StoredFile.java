package fi.publishertools.kss.model;

import java.time.Instant;

public class StoredFile {

    private final String id;
    private final String originalFilename;
    private final String contentType;
    private final long size;
    private final Instant uploadTime;
    private final byte[] data;

    public StoredFile(String id,
                      String originalFilename,
                      String contentType,
                      long size,
                      Instant uploadTime,
                      byte[] data) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.uploadTime = uploadTime;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public Instant getUploadTime() {
        return uploadTime;
    }

    public byte[] getData() {
        return data;
    }
}

