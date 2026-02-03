package fi.publishertools.kss.phases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.MandatoryMetadataMissingException;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Validates that all mandatory EPUB metadata fields are present in ProcessingContext.
 * If any are missing, throws MandatoryMetadataMissingException so the pipeline
 * can store the context for user interaction.
 */
public class CheckMandatoryInformationPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(CheckMandatoryInformationPhase.class);

    private static final List<String> MANDATORY_KEYS = Arrays.asList(
            "title", "creator", "publisher", "language", "identifier"
    );

    @Override
    public void process(ProcessingContext context) throws MandatoryMetadataMissingException {
        logger.debug("Checking mandatory metadata for file {}", context.getFileId());

        for (String key : MANDATORY_KEYS) {
            String value = context.getMetadata(key, String.class);
            if (value == null || value.trim().isEmpty()) {
                logger.info("Mandatory metadata field '{}' missing for file {}", key, context.getFileId());
                throw new MandatoryMetadataMissingException(context);
            }
        }

        logger.debug("All mandatory metadata present for file {}", context.getFileId());
    }

    /**
     * Returns the list of mandatory metadata keys.
     */
    public static List<String> getMandatoryKeys() {
        return Collections.unmodifiableList(MANDATORY_KEYS);
    }

    /**
     * Returns the list of mandatory keys that are missing or blank in the given context.
     */
    public static List<String> getMissingFields(ProcessingContext context) {
        List<String> missing = new ArrayList<>();
        for (String key : MANDATORY_KEYS) {
            String value = context.getMetadata(key, String.class);
            if (value == null || value.trim().isEmpty()) {
                missing.add(key);
            }
        }
        return missing;
    }
}
