package fi.publishertools.kss.phases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.exception.MandatoryMetadataMissingException;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Validates that all mandatory EPUB metadata fields are present in ProcessingContext.
 * If any are missing, throws MandatoryMetadataMissingException so the pipeline
 * can store the context for user interaction.
 */
public class B1_CheckMandatoryInformation extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(B1_CheckMandatoryInformation.class);

    private static final List<String> MANDATORY_KEYS = Arrays.asList(
            "title", "creator", "publisher", "language", "identifier"
    );

    @Override
    public void process(ProcessingContext context) throws MandatoryMetadataMissingException {
        logger.debug("Checking mandatory metadata for file {}", context.getFileId());

        List<String> missingFields = getMissingFields(context);
        List<String> missingImages = getMissingImages(context);

        if (!missingFields.isEmpty() || !missingImages.isEmpty()) {
            if (!missingFields.isEmpty()) {
                logger.info("Mandatory metadata fields missing for file {}: {}", context.getFileId(), missingFields);
            }
            if (!missingImages.isEmpty()) {
                logger.info("Image content missing for file {}: {}", context.getFileId(), missingImages);
            }
            throw new MandatoryMetadataMissingException(context);
        }

        logger.debug("All mandatory metadata and image content present for file {}", context.getFileId());
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

    /**
     * Returns the list of filenames that have no image content in the given context.
     * Returns unique filenames only.
     */
    public static List<String> getMissingImages(ProcessingContext context) {
        Set<String> missing = new LinkedHashSet<>();
        List<ImageNode> imageList = context.getImageList();
        if (imageList != null) {
            for (ImageNode info : imageList) {
                String fileName = info.fileName();
                if (fileName != null && !fileName.trim().isEmpty()) {
                    byte[] content = context.getImageContent(fileName);
                    if (content == null || content.length == 0) {
                        missing.add(fileName);
                    }
                }
            }
        }
        return new ArrayList<>(missing);
    }
}
