package fi.publishertools.kss.processing.phases;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Phase 0: Extract container.xml from ZIP, read first rootfile's media-type and full-path;
 * if media-type is "text/xml", extract the file at full-path, parse it, collect Story src
 * attributes, and pass the list to the next phase.
 */
public class ExtractStoriesPhase implements ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(ExtractStoriesPhase.class);

    private static final String CONTAINER_PATH = "META-INF/container.xml";
    private static final String CONTAINER_NS = "urn:oasis:names:tc:opendocument:xmlns:container";
    private static final String REQUIRED_MEDIA_TYPE = "text/xml";
    private static final String IDML_PACKAGING_NS = "http://ns.adobe.com/AdobeInDesign/idml/1.0/packaging";

    /** Metadata key for the list of Story {@code src} attribute values passed to the next phase. */
    public static final String STORY_SRC_LIST_KEY = "storySrcList";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Extracting stories for file {}", context.getFileId());

        byte[] zipBytes = context.getData();
        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalArgumentException("Input data is empty");
        }

        byte[] containerXml = extractZipEntry(zipBytes, CONTAINER_PATH);
        if (containerXml == null) {
            throw new IllegalArgumentException("ZIP does not contain " + CONTAINER_PATH);
        }

        Document doc = parseXml(containerXml);
        Element rootfile = findFirstRootfile(doc);
        String mediaType = rootfile.getAttribute("media-type");
        String fullPath = rootfile.getAttribute("full-path");

        if (fullPath == null || fullPath.isEmpty()) {
            throw new IllegalArgumentException("First rootfile has no full-path attribute");
        }

        if (!REQUIRED_MEDIA_TYPE.equals(mediaType)) {
            throw new IllegalArgumentException(
                    "First rootfile media-type is '" + mediaType + "', required is '" + REQUIRED_MEDIA_TYPE + "'");
        }

        byte[] extracted = extractZipEntry(zipBytes, fullPath);
        if (extracted == null) {
            // Try normalized path (forward slashes)
            String normalized = fullPath.replace('\\', '/');
            extracted = extractZipEntry(zipBytes, normalized);
        }
        if (extracted == null) {
            throw new IllegalArgumentException("ZIP does not contain entry at full-path: " + fullPath);
        }

        context.setData(extracted);
        context.addMetadata("rootfileMediaType", mediaType);
        context.addMetadata("rootfileFullPath", fullPath);

        Document fullPathDoc = parseXml(extracted);
        List<String> storySrcList = extractStorySrcList(fullPathDoc);
        context.addMetadata(STORY_SRC_LIST_KEY, storySrcList);

        logger.debug("Extracted file at {} (media-type {}), {} Story src entries for file {}",
                fullPath, mediaType, storySrcList.size(), context.getFileId());
    }

    @Override
    public String getName() {
        return "ExtractStories";
    }

    private static byte[] extractZipEntry(byte[] zipBytes, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.equals(entryName) || name.equals(entryName.replace('\\', '/'))) {
                    return zis.readAllBytes();
                }
            }
        }
        return null;
    }

    private static Document parseXml(byte[] xmlBytes) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = new ByteArrayInputStream(xmlBytes)) {
            return builder.parse(in);
        }
    }

    private static Element findFirstRootfile(Document doc) {
        NodeList list = doc.getElementsByTagNameNS(CONTAINER_NS, "rootfile");
        if (list.getLength() == 0) {
            throw new IllegalArgumentException("container.xml has no {" + CONTAINER_NS + "}rootfile element");
        }
        return (Element) list.item(0);
    }

    private static List<String> extractStorySrcList(Document doc) {
        NodeList stories = doc.getElementsByTagNameNS(IDML_PACKAGING_NS, "Story");
        List<String> srcList = new ArrayList<>(stories.getLength());
        for (int i = 0; i < stories.getLength(); i++) {
            Element story = (Element) stories.item(i);
            String src = story.getAttribute("src");
            srcList.add(src != null ? src : "");
        }
        return srcList;
    }
}
