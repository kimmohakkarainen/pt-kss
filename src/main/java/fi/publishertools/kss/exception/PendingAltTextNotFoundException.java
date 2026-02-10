package fi.publishertools.kss.exception;

/**
 * Thrown when a requested file ID is not found in the pending alt text review store.
 */
public class PendingAltTextNotFoundException extends RuntimeException {

    public PendingAltTextNotFoundException(String message) {
        super(message);
    }
}
