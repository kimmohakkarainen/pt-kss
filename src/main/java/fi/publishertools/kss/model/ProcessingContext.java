package fi.publishertools.kss.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object that carries file data and metadata through processing phases.
 */
public class ProcessingContext {

    private final String fileId;
    private final String originalFilename;
    private final String contentType;
    private final long fileSize;
    private final Instant uploadTime;
    private byte[] data;
    private final Map<String, Object> metadata;

    public ProcessingContext(StoredFile storedFile) {
        this.fileId = storedFile.getId();
        this.originalFilename = storedFile.getOriginalFilename();
        this.contentType = storedFile.getContentType();
        this.fileSize = storedFile.getSize();
        this.uploadTime = storedFile.getUploadTime();
        this.data = storedFile.getData();
        this.metadata = new HashMap<>();
    }

    public String getFileId() {
        return fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Instant getUploadTime() {
        return uploadTime;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
}
