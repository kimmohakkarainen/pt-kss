package fi.publishertools.kss.phases;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.util.XmlUtils;

/**
 * Takes the chapters from the context and generates a single XHTML document.
 * Renders ChapterNode hierarchy: text as paragraphs, images as figures, sections with optional titles.
 */
public class C1_GenerateXHTML extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(C1_GenerateXHTML.class);

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";
    private static final String IMAGES_PATH = "images/";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Generating XHTML for file {}", context.getFileId());

        List<ChapterNode> chapters = context.getChapters();
        if (chapters == null) {
            chapters = Collections.emptyList();
        }

        String title = context.getOriginalFilename() != null ? context.getOriginalFilename() : "";
        String escapedTitle = XmlUtils.escapeXml(title);

        StringBuilder body = new StringBuilder();
        SectionIdCounter counter = new SectionIdCounter();
        renderNodes(chapters, body, counter);

        String language = context.getMetadata("language", String.class);
        String langAttr = language != null ? language : "";

        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"" + XHTML_NS + "\" lang=\"" + XmlUtils.escapeXml(langAttr) + "\" xml:lang=\"" + XmlUtils.escapeXml(langAttr) + "\">\n"
                + "  <head>\n"
                + "    <meta charset=\"UTF-8\"/>\n"
                + "    <title>" + escapedTitle + "</title>\n"
                + "  </head>\n"
                + "  <body>\n"
                + body.toString()
                + "  </body>\n"
                + "</html>";

        context.setXhtmlContent(xhtml.getBytes(StandardCharsets.UTF_8));
        logger.debug("Generated XHTML with {} top-level chapter entries for file {}", chapters.size(), context.getFileId());
    }

    private static void renderNodes(List<ChapterNode> nodes, StringBuilder out, SectionIdCounter counter) {
        if (nodes == null) {
            return;
        }
        for (ChapterNode node : nodes) {
            renderNode(node, out, counter);
        }
    }

    private static void renderNode(ChapterNode node, StringBuilder out, SectionIdCounter counter) {
        if (node.isContainer()) {
            String sectionId = "section-" + counter.next();
            String dataAttrs = buildStyleDataAttributes(node);
            out.append("    <section class=\"chapter\" id=\"").append(sectionId).append("\"").append(dataAttrs).append(">\n");
            if (node.title() != null && !node.title().isEmpty()) {
                out.append("      <h2>").append(XmlUtils.escapeXml(node.title())).append("</h2>\n");
            }
            if (node instanceof ParagraphStyleRangeNode) {
                renderParagraphContent(node.children(), out);
            } else {
                renderNodes(node.children(), out, counter);
            }
            out.append("    </section>\n");
        } else if (node.isText()) {
            String sectionId = "section-" + counter.next();
            String dataAttrs = buildStyleDataAttributes(node);
            out.append("    <section class=\"chapter\" id=\"").append(sectionId).append("\"").append(dataAttrs).append(">")
                    .append("<p>").append(renderTextWithOptionalLang(node)).append("</p>")
                    .append("</section>\n");
        } else if (node.isImage()) {
            String sectionId = "section-" + counter.next();
            String dataAttrs = buildStyleDataAttributes(node);
            String src = IMAGES_PATH + XmlUtils.escapeXml(node.imageRef() != null ? node.imageRef() : "");
            String alt = "";
            if (node instanceof ImageNode img && img.alternateText() != null && !img.alternateText().isBlank()) {
                alt = XmlUtils.escapeXml(img.alternateText());
            }
            out.append("    <section class=\"chapter\" id=\"").append(sectionId).append("\"").append(dataAttrs).append(">")
                    .append("<figure><img src=\"").append(src).append("\" alt=\"").append(alt).append("\"/></figure>")
                    .append("</section>\n");
        }
    }

    /**
     * Renders a paragraph's children: consecutive CharacterStyleRangeNodes as one or more <p> with
     * optional <span lang="..."> for non-main-language segments; ImageNodes as <figure>. No extra sections.
     */
    private static void renderParagraphContent(List<ChapterNode> children, StringBuilder out) {
        if (children == null || children.isEmpty()) {
            return;
        }
        List<CharacterStyleRangeNode> textRun = new java.util.ArrayList<>();
        for (ChapterNode node : children) {
            if (node instanceof CharacterStyleRangeNode csr) {
                textRun.add(csr);
            } else {
                flushParagraphTextRun(textRun, out);
                textRun.clear();
                if (node instanceof ImageNode img) {
                    String src = IMAGES_PATH + XmlUtils.escapeXml(img.fileName() != null ? img.fileName() : "");
                    String alt = img.alternateText() != null && !img.alternateText().isBlank()
                            ? XmlUtils.escapeXml(img.alternateText()) : "";
                    out.append("      <figure><img src=\"").append(src).append("\" alt=\"").append(alt).append("\"/></figure>\n");
                }
            }
        }
        flushParagraphTextRun(textRun, out);
    }

    private static void flushParagraphTextRun(List<CharacterStyleRangeNode> run, StringBuilder out) {
        if (run.isEmpty()) {
            return;
        }
        out.append("      <p>");
        for (CharacterStyleRangeNode node : run) {
            out.append(renderTextWithOptionalLang(node));
        }
        out.append("</p>\n");
    }

    private static String renderTextWithOptionalLang(ChapterNode node) {
        if (!(node instanceof CharacterStyleRangeNode csr)) {
            return XmlUtils.escapeXml(node.text() != null ? node.text() : "");
        }
        String text = csr.text() != null ? csr.text() : "";
        String escaped = XmlUtils.escapeXml(text);
        String lang = csr.language();
        String style = csr.appliedStyle();
        boolean needSpan = (lang != null && !lang.isBlank()) || (style != null && !style.isEmpty());
        if (needSpan) {
            StringBuilder span = new StringBuilder("<span");
            if (lang != null && !lang.isBlank()) {
                String langEscaped = XmlUtils.escapeXml(lang);
                span.append(" lang=\"").append(langEscaped).append("\" xml:lang=\"").append(langEscaped).append("\"");
            }
            if (style != null && !style.isEmpty()) {
                span.append(" data-character-style=\"").append(XmlUtils.escapeXml(style)).append("\"");
            }
            span.append(">").append(escaped).append("</span>");
            return span.toString();
        }
        return escaped;
    }

    private static String buildStyleDataAttributes(ChapterNode node) {
        StringBuilder sb = new StringBuilder();
        String style = node.appliedStyle();
        if (style == null || style.isEmpty()) {
            return sb.toString();
        }
        String escaped = XmlUtils.escapeXml(style);
        if (node instanceof StoryNode) {
            sb.append(" data-toc-style=\"").append(escaped).append("\"");
        } else if (node instanceof ParagraphStyleRangeNode) {
            sb.append(" data-paragraph-style=\"").append(escaped).append("\"");
        } else if (node instanceof CharacterStyleRangeNode || node instanceof ImageNode) {
            sb.append(" data-character-style=\"").append(escaped).append("\"");
        }
        return sb.toString();
    }

    private static class SectionIdCounter {
        private int next = 1;

        int next() {
            return next++;
        }
    }
}
