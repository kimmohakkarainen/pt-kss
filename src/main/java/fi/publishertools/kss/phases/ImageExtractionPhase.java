package fi.publishertools.kss.phases;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fi.publishertools.kss.model.ImageInfo;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.util.XmlUtils;
import fi.publishertools.kss.util.ZipUtils;

/**
 * Phase that runs after ExtractChaptersPhase. Iterates over all XML story files listed in
 * processing context storiesList, parses each, finds Link elements, and extracts
 * LinkResourceURI and LinkResourceFormat attributes into imageList.
 */
public class ImageExtractionPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(ImageExtractionPhase.class);

    private static final String ATTR_LINK_RESOURCE_URI = "LinkResourceURI";
    private static final String ATTR_LINK_RESOURCE_FORMAT = "LinkResourceFormat";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Extracting images for file {}", context.getFileId());

        byte[] zipBytes = context.getOriginalFileContents();
        List<String> storySrcList = context.getStoriesList();

        List<ImageInfo> imageList = new ArrayList<>();
        if (zipBytes == null || zipBytes.length == 0 || storySrcList == null || storySrcList.isEmpty()) {
            context.setImageList(imageList);
            logger.debug("No ZIP or story list for file {}, image list empty", context.getFileId());
            return;
        }

        for (String storyPath : storySrcList) {
            String normalized = storyPath == null ? "" : storyPath.replace('\\', '/');
            byte[] storyBytes = ZipUtils.extractEntry(zipBytes, normalized);
            if (storyBytes == null && storyPath != null && !storyPath.isEmpty()) {
                storyBytes = ZipUtils.extractEntry(zipBytes, storyPath);
            }
            if (storyBytes == null) {
                logger.warn("Story entry not found in ZIP for file {}: {}", context.getFileId(), storyPath);
                continue;
            }
            try {
                Document doc = XmlUtils.parseXml(storyBytes);
                List<ImageInfo> images = collectImagesFromStoryDocument(doc);
                imageList.addAll(images);
            } catch (Exception e) {
                logger.warn("Failed to parse story XML for file {} entry {}: {}",
                        context.getFileId(), storyPath, e.getMessage());
            }
        }

        context.setImageList(imageList);

        // Populate imageContent from ZIP for each unique resource URI; key by filename
        Set<String> processedUris = new LinkedHashSet<>();
        for (ImageInfo info : imageList) {
            String uri = info.resourceUri();
            String fileName = info.fileName();
            if (uri != null && !uri.trim().isEmpty() && fileName != null && !fileName.isEmpty()
                    && !processedUris.contains(uri)) {
                processedUris.add(uri);
                byte[] bytes = extractEntryWithEncodedFallback(zipBytes, uri);
                if (bytes != null && bytes.length > 0) {
                    context.addImageContent(fileName, bytes);
                }
            }
        }

        logger.debug("Extracted {} image entries for file {} ({} with content from ZIP)", imageList.size(), context.getFileId(), context.getImageContent().size());
    }

    private static byte[] extractEntryWithEncodedFallback(byte[] zipBytes, String entryName) throws IOException {
        byte[] bytes = ZipUtils.extractEntry(zipBytes, entryName);
        if (bytes != null) {
            return bytes;
        }
        String encoded = encodeUriForZipLookup(entryName);
        if (!encoded.equals(entryName)) {
            return ZipUtils.extractEntry(zipBytes, encoded);
        }
        return null;
    }

    /**
     * Normalizes format string: trim and lowercase for consistent comparison.
     */
    private static String normalizeFormat(String format) {
        if (format == null) {
            return "";
        }
        return format.trim().toLowerCase();
    }

    /**
     * Returns MIME type for an image based on filename extension.
     * Comparison is case-insensitive. Returns application/octet-stream for unknown extensions.
     */
    public static String getMimeTypeFromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "application/octet-stream";
        }
        String ext = filename.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * Encodes a URI for ZIP lookup when the ZIP may store percent-encoded entry names.
     */
    private static String encodeUriForZipLookup(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        String[] segments = uri.split("/", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    /**
     * Find all Link elements in the document and extract LinkResourceURI and LinkResourceFormat.
     */
    private static List<ImageInfo> collectImagesFromStoryDocument(Document doc) {
        List<ImageInfo> result = new ArrayList<>();
        Element root = doc.getDocumentElement();
        if (root == null) {
            return result;
        }
        List<Element> links = XmlUtils.findElementsByLocalName(root, "Link");
        for (Element link : links) {
            String uri = link.getAttribute(ATTR_LINK_RESOURCE_URI);
            String decodedUri = ZipUtils.decodeUri(uri != null ? uri : "");
            String fileName = ZipUtils.extractFileNameFromUri(decodedUri);
            String format = link.getAttribute(ATTR_LINK_RESOURCE_FORMAT);
            String normalizedFormat = normalizeFormat(format != null ? format : "");
            result.add(new ImageInfo(decodedUri, fileName, normalizedFormat));
        }
        return result;
    }
}
