package fi.publishertools.kss.processing.phases;

import java.security.MessageDigest;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Phase 2: Transform data by adding processing timestamp and calculating checksum.
 */
public class DataTransformationPhase implements ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(DataTransformationPhase.class);

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Transforming data for file {}", context.getFileId());

        // Add processing timestamp
        Instant processingTime = Instant.now();
        context.addMetadata("processedAt", processingTime.toString());

        // Calculate checksum (SHA-256)
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(context.getData());
            String checksum = bytesToHex(hash);
            context.addMetadata("checksum", checksum);
            context.addMetadata("checksumAlgorithm", "SHA-256");
            logger.debug("Calculated checksum for file {}", context.getFileId());
        } catch (Exception e) {
            logger.warn("Failed to calculate checksum for file {}", context.getFileId(), e);
            // Continue processing even if checksum calculation fails
        }

        logger.debug("Data transformation completed for file {}", context.getFileId());
    }

    @Override
    public String getName() {
        return "DataTransformation";
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
