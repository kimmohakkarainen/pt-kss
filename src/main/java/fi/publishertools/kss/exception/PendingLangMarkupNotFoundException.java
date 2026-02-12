package fi.publishertools.kss.exception;

/**
 * Thrown when a requested file ID or occurrence index is not found in the pending lang markup review store.
 */
public class PendingLangMarkupNotFoundException extends RuntimeException {

    public PendingLangMarkupNotFoundException(String message) {
        super(message);
    }
}
