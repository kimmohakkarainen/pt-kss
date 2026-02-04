package fi.publishertools.kss.phases;

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

    private static final String ATTR_LIN_RESOURCE_URI = "LinResourceURI";
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
        logger.debug("Extracted {} image entries for file {}", imageList.size(), context.getFileId());
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
            String format = link.getAttribute(ATTR_LINK_RESOURCE_FORMAT);
            result.add(new ImageInfo(uri != null ? uri : "", format != null ? format : ""));
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
