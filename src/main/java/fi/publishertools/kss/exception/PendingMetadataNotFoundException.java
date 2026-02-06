package fi.publishertools.kss.exception;

/**
 * Thrown when a requested file ID is not found in the pending metadata store.
 */
public class PendingMetadataNotFoundException extends RuntimeException {

    public PendingMetadataNotFoundException(String message) {
        super(message);
    }
}
