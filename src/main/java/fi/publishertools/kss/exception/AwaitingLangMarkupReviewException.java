package fi.publishertools.kss.exception;

import fi.publishertools.kss.model.ProcessingContext;

/**
 * Thrown when language markup has been proposed and the pipeline should pause for user review.
 * The pipeline catches this and stores the context in PendingLangMarkupStore.
 */
public class AwaitingLangMarkupReviewException extends Exception {

    private final ProcessingContext context;

    public AwaitingLangMarkupReviewException(ProcessingContext context) {
        super("Awaiting lang markup review for file " + context.getFileId());
        this.context = context;
    }

    public ProcessingContext getContext() {
        return context;
    }
}
