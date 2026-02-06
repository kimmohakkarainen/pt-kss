package fi.publishertools.kss.phases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.util.XmlUtils;
import fi.publishertools.kss.util.ZipUtils;

/**
 * Phase 0: Extract container.xml from ZIP, read first rootfile's media-type and full-path;
 * if media-type is "text/xml", extract the file at full-path, parse it, collect Story src
 * attributes, and pass the list to the next phase.
 */
public class ExtractStoriesPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(ExtractStoriesPhase.class);

    private static final String CONTAINER_PATH = "META-INF/container.xml";
    private static final String CONTAINER_NS = "urn:oasis:names:tc:opendocument:xmlns:container";
    private static final String REQUIRED_MEDIA_TYPE = "text/xml";
    private static final String IDML_PACKAGING_NS = "http://ns.adobe.com/AdobeInDesign/idml/1.0/packaging";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Extracting stories for file {}", context.getFileId());

        byte[] zipBytes = context.getOriginalFileContents();
        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalArgumentException("Input data is empty");
        }

        byte[] containerXml = ZipUtils.extractEntry(zipBytes, CONTAINER_PATH);
        if (containerXml == null) {
            throw new IllegalArgumentException("ZIP does not contain " + CONTAINER_PATH);
        }

        Document doc = XmlUtils.parseXml(containerXml);
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

        byte[] extracted = ZipUtils.extractEntry(zipBytes, fullPath);
        if (extracted == null) {
            throw new IllegalArgumentException("ZIP does not contain entry at full-path: " + fullPath);
        }

        context.addMetadata("rootfileMediaType", mediaType);
        context.addMetadata("rootfileFullPath", fullPath);

        Document fullPathDoc = XmlUtils.parseXml(extracted);
        List<String> storySrcList = extractStorySrcList(fullPathDoc);
        context.setStoriesList(storySrcList);

        logger.debug("Extracted file at {} (media-type {}), {} Story src entries for file {}",
                fullPath, mediaType, storySrcList.size(), context.getFileId());
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
