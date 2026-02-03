package fi.publishertools.kss.dto;

/**
 * Summary of a pending metadata context for list endpoint.
 */
public class PendingMetadataSummary {

    private final String fileId;
    private final String originalFilename;

    public PendingMetadataSummary(String fileId, String originalFilename) {
        this.fileId = fileId;
        this.originalFilename = originalFilename;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }
}
