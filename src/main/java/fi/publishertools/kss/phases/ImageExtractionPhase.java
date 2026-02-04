package fi.publishertools.kss.phases;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fi.publishertools.kss.model.ImageInfo;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Phase that runs after ExtractChaptersPhase. Iterates over all XML story files listed in
 * processing context storiesList, parses each, finds Link elements, and extracts
 * LinResourceURI and LinkResourceFormat attributes into imageList.
 */
public class ImageExtractionPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(ImageExtractionPhase.class);

    private static final String ATTR_LIN_RESOURCE_URI = "LinkResourceURI";
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
            byte[] storyBytes = extractZipEntry(zipBytes, normalized);
            if (storyBytes == null && storyPath != null && !storyPath.isEmpty()) {
                storyBytes = extractZipEntry(zipBytes, storyPath);
            }
            if (storyBytes == null) {
                logger.warn("Story entry not found in ZIP for file {}: {}", context.getFileId(), storyPath);
                continue;
            }
            try {
                Document doc = parseXml(storyBytes);
                List<ImageInfo> images = collectImagesFromStoryDocument(doc);
                imageList.addAll(images);
            } catch (Exception e) {
                logger.warn("Failed to parse story XML for file {} entry {}: {}",
                        context.getFileId(), storyPath, e.getMessage());
            }
        }

        context.setImageList(imageList);

        // Populate imageContent from ZIP for each unique resource URI
        Set<String> uniqueUris = new LinkedHashSet<>();
        for (ImageInfo info : imageList) {
            String uri = info.resourceUri();
            if (uri != null && !uri.trim().isEmpty()) {
                uniqueUris.add(uri);
            }
        }
        for (String uri : uniqueUris) {
            byte[] bytes = extractZipEntryWithEncodedFallback(zipBytes, uri);
            if (bytes != null && bytes.length > 0) {
                context.addImageContent(uri, bytes);
            }
        }

        logger.debug("Extracted {} image entries for file {} ({} with content from ZIP)", imageList.size(), context.getFileId(), context.getImageContent().size());
    }

    private static byte[] extractZipEntryWithEncodedFallback(byte[] zipBytes, String entryName) throws IOException {
        byte[] bytes = extractZipEntry(zipBytes, entryName);
        if (bytes != null) {
            return bytes;
        }
        String encoded = encodeUriForZipLookup(entryName);
        if (!encoded.equals(entryName)) {
            return extractZipEntry(zipBytes, encoded);
        }
        return null;
    }

    private static byte[] extractZipEntry(byte[] zipBytes, String entryName) throws IOException {
        if (entryName == null || entryName.isEmpty()) {
            return null;
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                String normalized = entryName.replace('\\', '/');
                if (name.equals(entryName) || name.equals(normalized)) {
                    return zis.readAllBytes();
                }
            }
        }
        return null;
    }

    /**
     * Decodes percent-encoded characters in a URI (e.g. %20 -> space, %C3%A4 -> ä)
     * and normalizes to NFC form. Ensures filenames can be compared with user-uploaded
     * filenames that use real characters, including when Unicode representations differ.
     */
    private static String decodeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        try {
            String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            return Normalizer.normalize(decoded, Normalizer.Form.NFC);
        } catch (IllegalArgumentException e) {
            return uri;
        }
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

    private static Document parseXml(byte[] xmlBytes) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = new ByteArrayInputStream(xmlBytes)) {
            return builder.parse(in);
        }
    }

    /**
     * Find all Link elements in the document and extract LinResourceURI and LinkResourceFormat.
     */
    private static List<ImageInfo> collectImagesFromStoryDocument(Document doc) {
        List<ImageInfo> result = new ArrayList<>();
        Element root = doc.getDocumentElement();
        if (root == null) {
            return result;
        }
        List<Element> links = findElementsByLocalName(root, "Link");
        for (Element link : links) {
            String uri = link.getAttribute(ATTR_LIN_RESOURCE_URI);
            String decodedUri = decodeUri(uri != null ? uri : "");
            String format = link.getAttribute(ATTR_LINK_RESOURCE_FORMAT);
            result.add(new ImageInfo(decodedUri, format != null ? format : ""));
        }
        return result;
    }

    private static List<Element> findElementsByLocalName(Element root, String localName) {
        List<Element> out = new ArrayList<>();
        collectElementsByLocalName(root, localName, out);
        return out;
    }

    private static void collectElementsByLocalName(Element e, String localName, List<Element> out) {
        if (e != null && localName.equals(e.getLocalName())) {
            out.add(e);
        }
        if (e == null) {
            return;
        }
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node child = nl.item(i);
            if (child instanceof Element) {
                collectElementsByLocalName((Element) child, localName, out);
            }
        }
    }
}
