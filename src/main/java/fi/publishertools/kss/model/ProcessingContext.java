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
    private byte[] packageOpf;
    private List<String> storiesList;
    private List<String> chapters;
    private List<ImageInfo> imageList;
    private final Map<String, byte[]> imageContent;
    private byte[] xhtmlContent;
    private byte[] tocContent;
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
        this.imageList = null;
        this.imageContent = new HashMap<>();
        this.xhtmlContent = null;
        this.tocContent = null;
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

    public byte[] getPackageOpf() {
    	return this.packageOpf;
    }
    
    public void setPackageObf(byte [] packageOpf) {
    	this.packageOpf = packageOpf;
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

    public List<ImageInfo> getImageList() {
        return imageList;
    }

    public void setImageList(List<ImageInfo> imageList) {
        this.imageList = imageList;
    }

    /**
     * Returns the map of image content keyed by resource URI.
     * Images (not embedded in IDML) can be added later via {@link #addImageContent}.
     */
    public Map<String, byte[]> getImageContent() {
        return imageContent;
    }

    /**
     * Stores actual image bytes for the given resource URI.
     */
    public void addImageContent(String resourceUri, byte[] content) {
        if (resourceUri != null && content != null) {
            this.imageContent.put(resourceUri, content);
        }
    }

    /**
     * Returns image bytes for the given resource URI, or null if not present.
     */
    public byte[] getImageContent(String resourceUri) {
        return resourceUri != null ? this.imageContent.get(resourceUri) : null;
    }

    public byte [] getXhtmlContent() {
        return xhtmlContent;
    }

    public void setXhtmlContent(byte [] xhtmlContent) {
        this.xhtmlContent = xhtmlContent;
    }

    public byte[] getTocContent() {
        return tocContent;
    }

    public void setTocContent(byte[] tocContent) {
        this.tocContent = tocContent;
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
