package fi.publishertools.kss.phases;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
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
            String dataAttrs = buildStyleDataAttributes(node.appliedTOCStyle(), node.appliedParagraphStyle(), null);
            out.append("    <section class=\"chapter\" id=\"").append(sectionId).append("\"").append(dataAttrs).append(">\n");
            if (node.title() != null && !node.title().isEmpty()) {
                out.append("      <h2>").append(XmlUtils.escapeXml(node.title())).append("</h2>\n");
            }
            renderNodes(node.children(), out, counter);
            out.append("    </section>\n");
        } else if (node.isText()) {
            String sectionId = "section-" + counter.next();
            String dataAttrs = buildStyleDataAttributes(null, null, node.appliedCharacterStyle());
            out.append("    <section class=\"chapter\" id=\"").append(sectionId).append("\"").append(dataAttrs).append(">")
                    .append("<p>").append(XmlUtils.escapeXml(node.text() != null ? node.text() : "")).append("</p>")
                    .append("</section>\n");
        } else if (node.isImage()) {
            String sectionId = "section-" + counter.next();
            String dataAttrs = buildStyleDataAttributes(null, null, node.appliedCharacterStyle());
            String src = IMAGES_PATH + XmlUtils.escapeXml(node.imageRef() != null ? node.imageRef() : "");
            out.append("    <section class=\"chapter\" id=\"").append(sectionId).append("\"").append(dataAttrs).append(">")
                    .append("<figure><img src=\"").append(src).append("\" alt=\"\"/></figure>")
                    .append("</section>\n");
        }
    }

    private static String buildStyleDataAttributes(String appliedTOCStyle, String appliedParagraphStyle, String appliedCharacterStyle) {
        StringBuilder sb = new StringBuilder();
        if (appliedTOCStyle != null && !appliedTOCStyle.isEmpty()) {
            sb.append(" data-toc-style=\"").append(XmlUtils.escapeXml(appliedTOCStyle)).append("\"");
        }
        if (appliedParagraphStyle != null && !appliedParagraphStyle.isEmpty()) {
            sb.append(" data-paragraph-style=\"").append(XmlUtils.escapeXml(appliedParagraphStyle)).append("\"");
        }
        if (appliedCharacterStyle != null && !appliedCharacterStyle.isEmpty()) {
            sb.append(" data-character-style=\"").append(XmlUtils.escapeXml(appliedCharacterStyle)).append("\"");
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
