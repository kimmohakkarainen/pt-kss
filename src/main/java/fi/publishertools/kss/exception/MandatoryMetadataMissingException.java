package fi.publishertools.kss.exception;

import fi.publishertools.kss.model.ProcessingContext;

/**
 * Thrown when mandatory metadata fields are missing from ProcessingContext.
 * The pipeline catches this and stores the context for user interaction.
 */
public class MandatoryMetadataMissingException extends Exception {

    private final ProcessingContext context;

    public MandatoryMetadataMissingException(ProcessingContext context) {
        super("Mandatory metadata missing for file " + context.getFileId());
        this.context = context;
    }

    public ProcessingContext getContext() {
        return context;
    }
}
