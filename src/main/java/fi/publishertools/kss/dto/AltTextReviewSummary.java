package fi.publishertools.kss.dto;

/**
 * Summary of a file awaiting alt text review (for list endpoint).
 */
public class AltTextReviewSummary {

    private final String fileId;
    private final String originalFilename;

    public AltTextReviewSummary(String fileId, String originalFilename) {
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
