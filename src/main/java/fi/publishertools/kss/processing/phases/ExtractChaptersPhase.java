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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Phase 1: Read stories-list (ZIP entry names) from context, extract each story file from the
 * original ZIP in that order, parse as XML, and collect text from ParagraphStyleRange/Content
 * under Story into a single ordered list for downstream phases.
 */
public class ExtractChaptersPhase implements ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(ExtractChaptersPhase.class);

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Extracting chapters for file {}", context.getFileId());

        byte[] zipBytes = context.getOriginalFileContents();
        List<String> storySrcList = context.getStoriesList();

        List<String> contentTextsList = new ArrayList<>();
        if (zipBytes == null || zipBytes.length == 0 || storySrcList == null || storySrcList.isEmpty()) {
            context.setChapters(contentTextsList);
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
                List<String> texts = collectContentTextsFromStoryDocument(doc);
                contentTextsList.addAll(texts);
            } catch (Exception e) {
                logger.warn("Failed to parse story XML for file {} entry {}: {}",
                        context.getFileId(), storyPath, e.getMessage());
            }
        }

        context.setChapters(contentTextsList);
        logger.debug("Extracted {} content text entries for file {}", contentTextsList.size(), context.getFileId());
    }

    @Override
    public String getName() {
        return "ExtractChapters";
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
     * Collect Content text from a story document: for each Story, take direct-child
     * ParagraphStyleRange elements, then under each ParagraphStyleRange all Content
     * elements' text, in order.
     */
    private static List<String> collectContentTextsFromStoryDocument(Document doc) {
        List<String> result = new ArrayList<>();
        Element root = doc.getDocumentElement();
        if (root == null) {
            return result;
        }
        List<Element> stories = findElementsByLocalName(root, "Story");
        for (Element story : stories) {
            NodeList children = story.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n instanceof Element && "ParagraphStyleRange".equals(((Element) n).getLocalName())) {
                    Element range = (Element) n;
                    List<Element> contentEls = findElementsByLocalName(range, "Content");
                    for (Element content : contentEls) {
                        String text = content.getTextContent();
                        result.add(text != null ? text : "");
                    }
                }
            }
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
