package fi.publishertools.kss.phases;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fi.publishertools.kss.model.ChapterNode;
import fi.publishertools.kss.model.CharacterStyleRangeNode;
import fi.publishertools.kss.model.ImageNode;
import fi.publishertools.kss.model.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoryNode;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.util.XmlUtils;
import fi.publishertools.kss.util.ZipUtils;

/**
 * Extracts chapters from IDML story XML with hierarchical structure:
 * <ul>
 *   <li>Story starts a new hierarchy level (section with AppliedTOCStyle)</li>
 *   <li>ParagraphStyleRange starts a new hierarchy level (section with AppliedParagraphStyle)</li>
 *   <li>CharacterStyleRange is same level as siblings; creates text/image leaves with AppliedCharacterStyle</li>
 * </ul>
 */
public class ExtractChaptersPhase extends ProcessingPhase {

	private static final Logger logger = LoggerFactory.getLogger(ExtractChaptersPhase.class);

	private static final String ATTR_LINK_RESOURCE_URI = "LinkResourceURI";
	private static final String ATTR_APPLIED_TOC_STYLE = "AppliedTOCStyle";
	private static final String ATTR_APPLIED_PARAGRAPH_STYLE = "AppliedParagraphStyle";
	private static final String ATTR_APPLIED_CHARACTER_STYLE = "AppliedCharacterStyle";

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

	/**
	 * Traverses the document building a hierarchical ChapterNode tree from Story,
	 * ParagraphStyleRange, and CharacterStyleRange elements.
	 */
	private static List<ChapterNode> collectContentInDocumentOrder(Document doc) {
		List<ChapterNode> result = new ArrayList<>();
		Element root = doc.getDocumentElement();
		if (root == null) {
			return result;
		}
		List<Element> stories = XmlUtils.findElementsByLocalName(root, "Story");
		for (Element story : stories) {
			String appliedTOCStyle = getAttributeValue(story, ATTR_APPLIED_TOC_STYLE);
			List<ChapterNode> recursed = recurseNodes(story);
			result.add(new StoryNode(recursed, appliedTOCStyle));
		}
		return result;
	}


	private static List<ChapterNode> recurseNodes(Element node) {
		List<ChapterNode> recursed = new ArrayList<>();
		NodeList list = node.getChildNodes();
		for(int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if(child instanceof Element) {
				ChapterNode childNode =handleElement((Element)child);
				if(childNode != null) {
					recursed.add(childNode);
				}
			}
		}
		return recursed;
	}

	private static ChapterNode handleElement(Element element) {
		String localName = XmlUtils.getElementName(element);
		if ("Content".equals(localName)) {
			String text = element.getTextContent();
			return new CharacterStyleRangeNode(text != null ? text : "", null);
		} else if ("Link".equals(localName)) {
			String uri = element.getAttribute(ATTR_LINK_RESOURCE_URI);
			String decodedUri = ZipUtils.decodeUri(uri != null ? uri : "");
			String fileName = ZipUtils.extractFileNameFromUri(decodedUri);
			return new ImageNode(fileName, null);
		} else if ("CharacterStyleRange".equals(localName)) {
			String appliedCharacterStyle = getAttributeValue(element, ATTR_APPLIED_CHARACTER_STYLE);
			List<ChapterNode> children = recurseNodes(element);
			if (children.isEmpty()) {
				return null;
			}
			if (children.size() == 1) {
				ChapterNode child = children.get(0);
				if (child instanceof CharacterStyleRangeNode) {
					return new CharacterStyleRangeNode(child.text(), appliedCharacterStyle);
				}
				if (child instanceof ImageNode) {
					return new ImageNode(child.imageRef(), appliedCharacterStyle);
				}
			}
			return new ParagraphStyleRangeNode(children, appliedCharacterStyle);
		} else if ("ParagraphStyleRange".equals(localName)) {
			String appliedParagraphStyle = getAttributeValue(element, ATTR_APPLIED_PARAGRAPH_STYLE);
			return new ParagraphStyleRangeNode(recurseNodes(element), appliedParagraphStyle);
		} else {
			List<ChapterNode> contents = recurseNodes(element);
			if(contents.isEmpty()) {
				return null;
			} else if(contents.size() == 1) {
				return contents.get(0);
			} else {
				return new ParagraphStyleRangeNode(contents, null);
			}
		}
	}

	private static String getAttributeValue(Element e, String name) {
		String val = e.getAttribute(name);
		return (val != null && !val.isEmpty()) ? val : null;
	}
}
