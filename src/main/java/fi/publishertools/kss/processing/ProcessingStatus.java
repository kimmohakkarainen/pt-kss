package fi.publishertools.kss.processing;

public enum ProcessingStatus {
    IN_PROGRESS,
    AWAITING_METADATA,
    AWAITING_ALT_TEXTS,
    AWAITING_LANG_MARKUP_REVIEW,
    READY,
    ERROR
}
