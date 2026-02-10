package fi.publishertools.kss.phases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
 * Phase that runs after A3_ExtractImageInfo. Reorganizes the chapter content hierarchy
 * based on paragraph style definitions from IDML design (Resources/Styles.xml).
 * Uses BasedOn chains to determine hierarchy levels and nests content accordingly.
 * simplifyStyle reduces CharacterStyles to their parent (BasedOn) and ParagraphStyles
 * to the StyleExportTagMap exportType="EPUB" exportTag value.
 */
public class A4_ResolveContentHierarchy extends ProcessingPhase {

	private static final Logger logger = LoggerFactory.getLogger(A4_ResolveContentHierarchy.class);

	private static final String STYLES_XML_PATH = "Resources/Styles.xml";
	private static final String ATTR_SELF = "Self";
	private static final String ATTR_EXPORT_TYPE = "ExportType";
	private static final String ATTR_EXPORT_TAG = "ExportTag";
	private static final String EXPORT_TYPE_EPUB = "EPUB";
	private static final String PREFIX_CHARACTER_STYLE = "CharacterStyle/";
	private static final String PREFIX_PARAGRAPH_STYLE = "ParagraphStyle/";

	@Override
	public void process(ProcessingContext context) throws Exception, IOException {
		logger.debug("Resolving content hierarchy for file {}", context.getFileId());

		List<ChapterNode> chapters = context.getChapters();
		if (chapters == null || chapters.isEmpty()) {
			logger.debug("No chapters for file {}, nothing to resolve", context.getFileId());
			return;
		}

		StyleMaps styleMaps = StyleMaps.loadStyleMaps(context);
		List<ChapterNode> reorganized = simplifyStyles(chapters, styleMaps);

		StyleTable table = collectStyles(reorganized, new StyleTable());

		logger.info(table.toString());

		context.setChapters(reorganized);
		logger.debug("Resolved content hierarchy for file {}", context.getFileId());
	}


	private List<ChapterNode> simplifyStyles(List<ChapterNode> input, StyleMaps maps) {
		List<ChapterNode> output = new ArrayList<>();

		for (ChapterNode node : input) {
			if (node instanceof StoryNode) {
				List<ChapterNode> children = simplifyStyles(node.children(), maps);
				if (children != null) {
					output.add(new StoryNode(children, maps.simplify(node.appliedStyle())));
				}
			} else if (node instanceof ParagraphStyleRangeNode) {
				List<ChapterNode> children = simplifyStyles(node.children(), maps);
				if (children != null) {
					output.add(new ParagraphStyleRangeNode(children, maps.simplify(node.appliedStyle())));
				}
			} else if (node instanceof CharacterStyleRangeNode) {
				output.add(new CharacterStyleRangeNode(node.text(), maps.simplify(node.appliedStyle())));
			} else if (node instanceof ImageNode img) {
				output.add(new ImageNode(img.resourceUri(), img.fileName(), img.resourceFormat(), maps.simplify(node.appliedStyle()), img.alternateText()));
			} else {
				// do nothing
			}
		}
		return output.isEmpty() ? null : output;
	}

	private static class StyleMaps {
		final Map<String, String> styleMap = new HashMap<>();

		public String simplify(String style) {
			style = fallbackLastSegment(style);
			if(style == null) {
				return null;
			} else if(styleMap.containsKey(style)) {
				return styleMap.get(style);
			} else {
				return style;
			}
		}

		private static StyleMaps loadStyleMaps(ProcessingContext context) {
 			StyleMaps maps = new StyleMaps();
			try {
				byte[] zipBytes = context.getOriginalFileContents();
				if (zipBytes == null || zipBytes.length == 0) {
					return maps;
				}
				byte[] stylesXml = ZipUtils.extractEntry(zipBytes, STYLES_XML_PATH);
				if (stylesXml == null || stylesXml.length == 0) {
					return maps;
				}
				Document stylesDoc = XmlUtils.parseXml(stylesXml);
				Element root = stylesDoc.getDocumentElement();
				if (root == null) {
					return maps;
				}

				for (Element el : XmlUtils.findElementsByLocalName(root, "CharacterStyle")) {
					String self = fallbackLastSegment(getAttribute(el, ATTR_SELF));
					String basedOn = fallbackLastSegment(findCharacterStyleBasedOn(el));
					if (self != null && basedOn != null && !basedOn.isEmpty()) {
						while(maps.styleMap.containsKey(basedOn)) {
							basedOn = maps.styleMap.get(basedOn);
						}
						maps.styleMap.put(self, basedOn);
					}
				}

				for (Element el : XmlUtils.findElementsByLocalName(root, "ParagraphStyle")) {
					String self = fallbackLastSegment(getAttribute(el, ATTR_SELF));
					String basedOn = fallbackLastSegment(findCharacterStyleBasedOn(el));
					if (self != null && basedOn != null && !basedOn.isEmpty()) {
						while(maps.styleMap.containsKey(basedOn)) {
							basedOn = maps.styleMap.get(basedOn);
						}
						maps.styleMap.put(self, basedOn);
					}
					String exportTag = findEpubExportTag(el);
					if (self != null && exportTag != null) {
						maps.styleMap.put(self, exportTag);
					}
				}
			} catch (Exception e) {
				logger.warn("Failed to load Styles.xml for file {}: {}", context.getFileId(), e.getMessage());
			}
			return maps;
		}

		private static String fallbackLastSegment(String style) {
			if(style == null) {
				return null;
			} else  {
				String[] splitted = style.split("/");
				return splitted.length > 0 ? splitted[splitted.length - 1] : style;
			}
		}


		/**
		 * Finds BasedOn from CharacterStyle. In IDML, BasedOn is a child of Properties, not an attribute.
		 * Structure: CharacterStyle -> Properties -> BasedOn (text content is the parent style Self).
		 */
		private static String findCharacterStyleBasedOn(Element characterStyle) {
			for (Element props : XmlUtils.findElementsByLocalName(characterStyle, "Properties")) {
				for (Element basedOn : XmlUtils.findElementsByLocalName(props, "BasedOn")) {
					String value = basedOn.getTextContent();
					if (value != null && !value.trim().isEmpty()) {
						return value.trim();
					}
				}
			}
			return null;
		}

		private static String findEpubExportTag(Element paragraphStyle) {
			for (Element child : XmlUtils.findElementsByLocalName(paragraphStyle, "StyleExportTagMap")) {
				String exportType = getAttribute(child, ATTR_EXPORT_TYPE);
				if (EXPORT_TYPE_EPUB.equals(exportType)) {
					String exportTag = getAttribute(child, ATTR_EXPORT_TAG);
					if (exportTag != null && !exportTag.isEmpty()) {
						return exportTag;
					}
				}
			}
			return null;
		}

		private static String getAttribute(Element el, String name) {
			String val = el.getAttribute(name);
			if (val != null && !val.isEmpty()) {
				return val;
			}
			return el.getAttributeNS(null, name);
		}
	}

	private StyleTable collectStyles(List<ChapterNode> input, StyleTable output) {

		for(ChapterNode node : input) {
			if(node.appliedStyle() != null) {
				output.add(node.appliedStyle());
			}
			if(node instanceof StoryNode) {
				output.push();
				output = collectStyles(node.children(), output);
				output.pop();
			} else if(node instanceof ParagraphStyleRangeNode) {
				output.push();
				output = collectStyles(node.children(), output);
				output.pop();
			}  
		}
		return output;
	}

	public static class StyleTable {

		private Map<String, Map<String, Long>> table = new TreeMap<>();
		private String previous = "level-0";
		private Stack<String> stack = new Stack<>();


		public void add(String next) {
			put(this.previous, next);
			this.previous = next;
		}

		public void addAll(Collection<String> col) {
			for(String next : col) {
				this.add(next);
			}
		}

		public void push() {
			stack.push(this.previous);
			this.previous = "level-" + Integer.toString(stack.size());
		}

		public void pop() {
			this.previous = stack.pop();
		}


		private void put(String previous, String next) {
			checkExistence(previous, next);
			Map<String, Long> row = table.get(previous);
			row.put(next,  1L + row.get(next));
		}

		private void checkExistence(String previous, String next) {
			checkExistence(previous);
			checkExistence(next);
		}

		private void checkExistence(String word) {
			Map<String, Long> row = table.get(word);
			if(row == null) {
				row = new TreeMap<>();
				table.put(word, row);
				for(String key : table.keySet()) {
					row.put(key, 0L);
					table.get(key).put(word, 0L);
				}
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("\n,");
			for(String col: table.keySet()) {
				builder.append(col);
				builder.append(",");
			}
			builder.append("\n");
			for(String row: table.keySet()) {
				builder.append(row);
				builder.append(",");
				for(String col: table.keySet()) {
					builder.append(table.get(row).get(col));
					builder.append(",");
				}
				builder.append("\n");
			}
			return builder.toString();
		}
	}
}
