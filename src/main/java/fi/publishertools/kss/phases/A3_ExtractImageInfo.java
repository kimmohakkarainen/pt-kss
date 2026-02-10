package fi.publishertools.kss.phases;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.serialization.ProcessingContextSerializer;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.util.XmlUtils;
import fi.publishertools.kss.util.ZipUtils;

/**
 * Phase that runs after ExtractChaptersPhase. Iterates over all story Documents in
 * processing context storiesList, finds Link elements in each, and extracts
 * LinkResourceURI and LinkResourceFormat attributes into imageList.
 */
public class A3_ExtractImageInfo extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(A3_ExtractImageInfo.class);

    private static final String KSS_A3_OUTPUT_PATH = "./a3-context.object";
    private static final String ATTR_LINK_RESOURCE_URI = "LinkResourceURI";
    private static final String ATTR_LINK_RESOURCE_FORMAT = "LinkResourceFormat";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Extracting images for file {}", context.getFileId());

        byte[] zipBytes = context.getOriginalFileContents();
        List<Document> storyDocs = context.getStoriesList();

        List<ImageNode> imageList = new ArrayList<>();
        if (storyDocs == null || storyDocs.isEmpty()) {
            context.setImageList(imageList);
            logger.debug("No story documents for file {}, image list empty", context.getFileId());
            serializeIfConfigured(context);
            return;
        }

        for (Document doc : storyDocs) {
            List<ImageNode> images = collectImagesFromStoryDocument(doc);
            imageList.addAll(images);
        }

        context.setImageList(imageList);

        // Populate imageContent from ZIP for each unique resource URI; key by filename
        Set<String> processedUris = new LinkedHashSet<>();
        if (zipBytes != null && zipBytes.length > 0) {
        for (ImageNode info : imageList) {
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
        }

        logger.debug("Extracted {} image entries for file {} ({} with content from ZIP)", imageList.size(), context.getFileId(), context.getImageContent().size());

        serializeIfConfigured(context);
    }

    private static void serializeIfConfigured(ProcessingContext context) {
        if (KSS_A3_OUTPUT_PATH != null) {
            try {
                ProcessingContextSerializer.serialize(context, Path.of(KSS_A3_OUTPUT_PATH));
                logger.debug("Serialized ProcessingContext to {} for file {}", KSS_A3_OUTPUT_PATH, context.getFileId());
            } catch (Exception e) {
                logger.warn("Failed to serialize ProcessingContext to {} for file {}: {}", KSS_A3_OUTPUT_PATH, context.getFileId(), e.getMessage());
            }
        }
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
    private static List<ImageNode> collectImagesFromStoryDocument(Document doc) {
        List<ImageNode> result = new ArrayList<>();
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
            result.add(new ImageNode(decodedUri, fileName, normalizedFormat, null));
        }
        return result;
    }
}
