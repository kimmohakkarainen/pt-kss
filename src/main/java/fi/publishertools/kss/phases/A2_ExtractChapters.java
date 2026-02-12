package fi.publishertools.kss.phases;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
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
public class A2_ExtractChapters extends ProcessingPhase {

	private static final Logger logger = LoggerFactory.getLogger(A2_ExtractChapters.class);

	private static final String ATTR_LINK_RESOURCE_URI = "LinkResourceURI";
	private static final String ATTR_APPLIED_TOC_STYLE = "AppliedTOCStyle";
	private static final String ATTR_APPLIED_PARAGRAPH_STYLE = "AppliedParagraphStyle";
	private static final String ATTR_APPLIED_CHARACTER_STYLE = "AppliedCharacterStyle";

	@Override
	public void process(ProcessingContext context) throws Exception {
		logger.debug("Extracting chapters for file {}", context.getFileId());

		List<Document> storyDocs = context.getStoriesList();

		List<ChapterNode> contentList = new ArrayList<>();
		if (storyDocs == null || storyDocs.isEmpty()) {
			context.setChapters(contentList);
			logger.debug("No story documents for file {}, content list empty", context.getFileId());
			return;
		}

		for (Document doc : storyDocs) {
			List<ChapterNode> nodes = collectContentInDocumentOrder(doc);
			contentList.addAll(nodes);
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
			String appliedStyle = getAttributeValue(story, ATTR_APPLIED_TOC_STYLE);
			List<ChapterNode> recursed = recurseNodes(story);
			result.add(new StoryNode(recursed, appliedStyle));
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
			return new CharacterStyleRangeNode(text != null ? text : "", null, null);
		} else if ("Link".equals(localName)) {
			String uri = element.getAttribute(ATTR_LINK_RESOURCE_URI);
			String decodedUri = ZipUtils.decodeUri(uri != null ? uri : "");
			String fileName = ZipUtils.extractFileNameFromUri(decodedUri);
			return new ImageNode(null, fileName, null, null, null);
		} else if ("CharacterStyleRange".equals(localName)) {
			String appliedStyle = getAttributeValue(element, ATTR_APPLIED_CHARACTER_STYLE);
			List<ChapterNode> children = recurseNodes(element);
			if (children.isEmpty()) {
				return null;
			}
			if (children.size() == 1) {
				ChapterNode child = children.get(0);
				if (child instanceof CharacterStyleRangeNode) {
					return new CharacterStyleRangeNode(child.text(), appliedStyle, null);
				}
				if (child instanceof ImageNode img) {
					return new ImageNode(img.resourceUri(), img.fileName(), img.resourceFormat(), appliedStyle, img.alternateText());
				}
			}
			return new ParagraphStyleRangeNode(children, appliedStyle);
		} else if ("ParagraphStyleRange".equals(localName)) {
			String appliedStyle = getAttributeValue(element, ATTR_APPLIED_PARAGRAPH_STYLE);
			return new ParagraphStyleRangeNode(recurseNodes(element), appliedStyle);
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
