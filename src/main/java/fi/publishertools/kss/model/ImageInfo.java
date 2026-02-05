package fi.publishertools.kss.model;

/**
 * Holds resource URI, resolved filename, and format for an image extracted from Link elements in story XML.
 * All fields are decoded and normalized at extraction time.
 *
 * @param resourceUri  Full path (decoded, NFC-normalized); may be needed for ZIP lookup or path resolution
 * @param fileName    Last path segment resolved from the URI; used as canonical key for image content
 * @param resourceFormat Normalized format (e.g. MIME type)
 */
public record ImageInfo(String resourceUri, String fileName, String resourceFormat) {}
