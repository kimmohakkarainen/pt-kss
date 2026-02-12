package fi.publishertools.kss.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of a file awaiting language markup review")
public class LangMarkupReviewSummary {

    private final String fileId;
    private final String originalFilename;

    public LangMarkupReviewSummary(String fileId, String originalFilename) {
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
