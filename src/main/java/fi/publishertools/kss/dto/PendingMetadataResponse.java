package fi.publishertools.kss.dto;

import java.util.List;
import java.util.Map;

/**
 * Response for GET pending-metadata/{fileId} with current metadata and missing fields.
 */
public class PendingMetadataResponse {

    private final String fileId;
    private final String originalFilename;
    private final Map<String, Object> metadata;
    private final List<String> missingFields;

    public PendingMetadataResponse(String fileId, String originalFilename,
                                   Map<String, Object> metadata, List<String> missingFields) {
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.metadata = metadata;
        this.missingFields = missingFields;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
