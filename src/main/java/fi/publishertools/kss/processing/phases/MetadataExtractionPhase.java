package fi.publishertools.kss.processing.phases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Phase 1: Extract metadata from the uploaded file.
 */
public class MetadataExtractionPhase implements ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(MetadataExtractionPhase.class);

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Extracting metadata for file {}", context.getFileId());

        // Extract and store metadata
        context.addMetadata("extractedAt", java.time.Instant.now().toString());
        context.addMetadata("originalFilename", context.getOriginalFilename());
        context.addMetadata("contentType", context.getContentType());
        context.addMetadata("fileSize", context.getFileSize());
        context.addMetadata("uploadTime", context.getUploadTime().toString());

        logger.debug("Metadata extraction completed for file {}", context.getFileId());
    }

    @Override
    public String getName() {
        return "MetadataExtraction";
    }
}
