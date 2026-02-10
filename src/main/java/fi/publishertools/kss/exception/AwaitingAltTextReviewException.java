package fi.publishertools.kss.exception;

import fi.publishertools.kss.model.ProcessingContext;

/**
 * Thrown when alt text has been proposed and the pipeline should pause for user review.
 * The pipeline catches this and stores the context in PendingAltTextStore.
 */
public class AwaitingAltTextReviewException extends Exception {

    private final ProcessingContext context;

    public AwaitingAltTextReviewException(ProcessingContext context) {
        super("Awaiting alt text review for file " + context.getFileId());
        this.context = context;
    }

    public ProcessingContext getContext() {
        return context;
    }
}
