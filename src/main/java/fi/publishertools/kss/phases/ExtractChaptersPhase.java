package fi.publishertools.kss.phases;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fi.publishertools.kss.model.ChapterNode;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Extracts chapters from IDML story XML: text from ParagraphStyleRange/Content and image references
 * from Link elements, in document order. Produces a flat list of ChapterNode (text and image leaves).
 */
public class ExtractChaptersPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(ExtractChaptersPhase.class);

    private static final String ATTR_LINK_RESOURCE_URI = "LinkResourceURI";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Extracting chapters for file {}", context.getFileId());

        byte[] zipBytes = context.getOriginalFileContents();
        List<String> storySrcList = context.getStoriesList();

        List<ChapterNode> contentList = new ArrayList<>();
        if (zipBytes == null || zipBytes.length == 0 || storySrcList == null || storySrcList.isEmpty()) {
            context.setChapters(contentList);
            logger.debug("No ZIP or story list for file {}, content list empty", context.getFileId());
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
                List<ChapterNode> nodes = collectContentInDocumentOrder(doc);
                contentList.addAll(nodes);
            } catch (Exception e) {
                logger.warn("Failed to parse story XML for file {} entry {}: {}",
                        context.getFileId(), storyPath, e.getMessage());
            }
        }

        context.setChapters(contentList);
        logger.debug("Extracted {} content entries for file {}", contentList.size(), context.getFileId());
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

    private static Document parseXml(byte[] xmlBytes) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = new ByteArrayInputStream(xmlBytes)) {
            return builder.parse(in);
        }
    }

    /**
     * Traverses the document in DOM order, collecting text from Content elements and image refs
     * from Link elements into a flat list of ChapterNode.
     */
    private static List<ChapterNode> collectContentInDocumentOrder(Document doc) {
        List<ChapterNode> result = new ArrayList<>();
        Element root = doc.getDocumentElement();
        if (root == null) {
            return result;
        }
        List<Element> stories = findElementsByLocalName(root, "Story");
        for (Element story : stories) {
            collectFromElement(story, result);
        }
        return result;
    }

    private static void collectFromElement(Element e, List<ChapterNode> out) {
        if (e == null) {
            return;
        }
        String localName = e.getLocalName();
        if ("Content".equals(localName)) {
            String text = e.getTextContent();
            out.add(ChapterNode.text(text != null ? text : ""));
            return;
        }
        if ("Link".equals(localName)) {
            String uri = e.getAttribute(ATTR_LINK_RESOURCE_URI);
            String decodedUri = decodeUri(uri != null ? uri : "");
            String fileName = extractFileNameFromUri(decodedUri);
            if (fileName != null && !fileName.isEmpty()) {
                out.add(ChapterNode.image(fileName));
            }
            return;
        }
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node child = nl.item(i);
            if (child instanceof Element) {
                collectFromElement((Element) child, out);
            }
        }
    }

    private static String decodeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        try {
            String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            return Normalizer.normalize(decoded, Normalizer.Form.NFC);
        } catch (IllegalArgumentException ex) {
            return uri;
        }
    }

    private static String extractFileNameFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        int lastSlash = uri.lastIndexOf('/');
        return lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
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
