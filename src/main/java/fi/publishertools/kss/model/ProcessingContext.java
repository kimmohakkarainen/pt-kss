package fi.publishertools.kss.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
    private final byte[] originalFileContents;
    private List<String> storiesList;
    private List<String> chapters;
    private String xhtmlContent;
    private final Map<String, Object> metadata;

    public ProcessingContext(StoredFile storedFile) {
        this.fileId = storedFile.getId();
        this.originalFilename = storedFile.getOriginalFilename();
        this.contentType = storedFile.getContentType();
        this.fileSize = storedFile.getSize();
        this.uploadTime = storedFile.getUploadTime();
        this.originalFileContents = storedFile.getData();
        this.storiesList = null;
        this.chapters = null;
        this.xhtmlContent = null;
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

    public byte[] getOriginalFileContents() {
        return originalFileContents;
    }

    public List<String> getStoriesList() {
        return storiesList;
    }

    public void setStoriesList(List<String> storiesList) {
        this.storiesList = storiesList;
    }

    public List<String> getChapters() {
        return chapters;
    }

    public void setChapters(List<String> chapters) {
        this.chapters = chapters;
    }

    public String getXhtmlContent() {
        return xhtmlContent;
    }

    public void setXhtmlContent(String xhtmlContent) {
        this.xhtmlContent = xhtmlContent;
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
