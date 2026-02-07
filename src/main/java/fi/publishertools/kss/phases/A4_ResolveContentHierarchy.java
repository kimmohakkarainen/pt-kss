package fi.publishertools.kss.phases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.util.XmlUtils;
import fi.publishertools.kss.util.ZipUtils;

/**
 * Phase that runs after A3_ExtractImageInfo. Reorganizes the chapter content hierarchy
 * based on paragraph style definitions from IDML design (Resources/Styles.xml).
 * Uses BasedOn chains to determine hierarchy levels and nests content accordingly.
 */
public class A4_ResolveContentHierarchy extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(A4_ResolveContentHierarchy.class);

    private static final String RESOURCES_STYLES_PATH = "Resources/Styles.xml";
    private static final String PARAGRAPH_STYLE = "ParagraphStyle";
    private static final String ATTR_SELF = "Self";
    private static final String ATTR_BASED_ON = "BasedOn";
    /** Level for styles without a definition (body content). */
    private static final int DEFAULT_BODY_LEVEL = 999;

    @Override
    public void process(ProcessingContext context) throws Exception, IOException {
        logger.debug("Resolving content hierarchy for file {}", context.getFileId());

        List<ChapterNode> chapters = context.getChapters();
        if (chapters == null || chapters.isEmpty()) {
            logger.debug("No chapters for file {}, nothing to resolve", context.getFileId());
            return;
        }

        Map<String, Integer> styleToLevel = loadStyleLevels(context);
        List<ChapterNode> reorganized = reorganizeChapters(chapters, styleToLevel);
        context.setChapters(reorganized);
        logger.debug("Resolved content hierarchy for file {}", context.getFileId());
    }

    /**
     * Loads paragraph style hierarchy from IDML Resources/Styles.xml.
     * Returns a map from style ID (e.g. "ParagraphStyle/Heading1") to numeric level.
     * Root styles get level 1, children get 2, etc. Styles not found get DEFAULT_BODY_LEVEL.
     */
    private Map<String, Integer> loadStyleLevels(ProcessingContext context) throws Exception {
        Map<String, Integer> result = new HashMap<>();
        byte[] zipBytes = context.getOriginalFileContents();
        if (zipBytes == null || zipBytes.length == 0) {
            return result;
        }

        byte[] stylesXml = ZipUtils.extractEntry(zipBytes, RESOURCES_STYLES_PATH);
        if (stylesXml == null) {
            logger.debug("No Resources/Styles.xml in package for file {}, hierarchy unchanged", context.getFileId());
            return result;
        }

        Document doc = XmlUtils.parseXml(stylesXml);
        Element root = doc.getDocumentElement();
        if (root == null) {
            return result;
        }

        List<Element> paragraphStyles = XmlUtils.findElementsByLocalName(root, PARAGRAPH_STYLE);
        Map<String, String> styleToBasedOn = new HashMap<>();
        for (Element ps : paragraphStyles) {
            String self = getAttribute(ps, ATTR_SELF);
            String basedOn = getAttribute(ps, ATTR_BASED_ON);
            if (self != null && !self.isEmpty()) {
                styleToBasedOn.put(self, basedOn);
            }
        }

        for (String styleId : styleToBasedOn.keySet()) {
            int level = computeLevel(styleId, styleToBasedOn);
            result.put(styleId, level);
        }
        logger.debug("Loaded {} paragraph style levels from Resources/Styles.xml", result.size());
        return result;
    }

    private static int computeLevel(String styleId, Map<String, String> styleToBasedOn) {
        String current = styleId;
        int depth = 1;
        int maxIterations = 100;
        while (maxIterations-- > 0) {
            String base = styleToBasedOn.get(current);
            if (base == null || base.isEmpty() || base.equals(current)) {
                return depth;
            }
            if (base.contains("[No paragraph style]") || base.contains("[No Paragraph Style]")) {
                return depth;
            }
            current = base;
            depth++;
        }
        return depth;
    }

    private static String getAttribute(Element e, String name) {
        String val = e.getAttribute(name);
        return (val != null && !val.isEmpty()) ? val : null;
    }

    private List<ChapterNode> reorganizeChapters(List<ChapterNode> chapters, Map<String, Integer> styleToLevel) {
        List<ChapterNode> result = new ArrayList<>(chapters.size());
        for (ChapterNode node : chapters) {
            if (node instanceof StoryNode story) {
                List<ChapterNode> reorganizedChildren = reorganizeStoryChildren(story.children(), styleToLevel);
                result.add(new StoryNode(reorganizedChildren, story.appliedStyle()));
            } else {
                result.add(node);
            }
        }
        return result;
    }

    private List<ChapterNode> reorganizeStoryChildren(List<ChapterNode> children, Map<String, Integer> styleToLevel) {
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        if (styleToLevel.isEmpty()) {
            return new ArrayList<>(children);
        }

        List<ContentItem> flat = flattenToItems(children, styleToLevel);
        TreeBuildResult result = rebuildTree(flat, 0, 0);
        return result.nodes();
    }

    private record ContentItem(ChapterNode node, int level) {}

    private List<ContentItem> flattenToItems(List<ChapterNode> children, Map<String, Integer> styleToLevel) {
        List<ContentItem> result = new ArrayList<>();
        for (ChapterNode node : children) {
            int level = getLevel(node, styleToLevel);
            result.add(new ContentItem(node, level));
        }
        return result;
    }

    private int getLevel(ChapterNode node, Map<String, Integer> styleToLevel) {
        if (node instanceof ParagraphStyleRangeNode psr) {
            String style = psr.appliedStyle();
            if (style != null && styleToLevel.containsKey(style)) {
                return styleToLevel.get(style);
            }
        }
        return DEFAULT_BODY_LEVEL;
    }

    /**
     * Recursively builds tree from flat items. Processes items until one has level <= currentLevel
     * (which belongs to a sibling, not this section). Returns the built nodes and the next index.
     */
    private TreeBuildResult rebuildTree(List<ContentItem> items, int start, int currentLevel) {
        List<ChapterNode> result = new ArrayList<>();
        int i = start;
        while (i < items.size()) {
            ContentItem item = items.get(i);
            if (item.level() <= currentLevel) {
                break;
            }
            ChapterNode node = item.node();
            if (node instanceof ParagraphStyleRangeNode psr && psr.isContainer()) {
                TreeBuildResult childResult = rebuildTree(items, i + 1, item.level());
                List<ChapterNode> newChildren = new ArrayList<>(psr.children());
                newChildren.addAll(childResult.nodes());
                result.add(new ParagraphStyleRangeNode(newChildren, psr.appliedStyle()));
                i = childResult.nextIndex();
            } else {
                result.add(node);
                i++;
            }
        }
        return new TreeBuildResult(result, i);
    }

    private record TreeBuildResult(List<ChapterNode> nodes, int nextIndex) {}

}
