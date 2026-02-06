package fi.publishertools.kss.phases;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ChapterNode;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.util.XmlUtils;

/**
 * Generates XHTML table-of-contents content for the EPUB.
 * Uses chapters from the context to build a nav element with epub:type="toc"
 * and links to each section in the main content. Supports nested structure.
 */
public class GenerateTOCPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(GenerateTOCPhase.class);

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";
    private static final String EPUB_NS = "http://www.idpf.org/2007/ops";
    private static final String CONTENT_FILE = "Koottu-1.xhtml";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Generating TOC for file {}", context.getFileId());

        List<ChapterNode> chapters = context.getChapters();
        if (chapters == null) {
            chapters = Collections.emptyList();
        }

        StringBuilder ol = new StringBuilder();
        TocBuilder builder = new TocBuilder();
        buildToc(chapters, ol, builder);

        String language = context.getMetadata("language", String.class);
        String langAttr = language != null ? language : "";

        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"" + XHTML_NS + "\" xmlns:epub=\"" + EPUB_NS + "\" lang=\"" + XmlUtils.escapeXml(langAttr) + "\" xml:lang=\"" + XmlUtils.escapeXml(langAttr) + "\">\n"
                + "  <head>\n"
                + "    <meta charset=\"UTF-8\"/>\n"
                + "    <title>Table of Contents</title>\n"
                + "  </head>\n"
                + "  <body>\n"
                + "    <nav epub:type=\"toc\">\n"
                + "      <h2>Table of Contents</h2>\n"
                + "      <ol>\n"
                + ol.toString()
                + "      </ol>\n"
                + "    </nav>\n"
                + "  </body>\n"
                + "</html>";

        context.setTocContent(xhtml.getBytes(StandardCharsets.UTF_8));
        logger.debug("Generated TOC with {} entries for file {}", builder.count(), context.getFileId());
    }

    private static void buildToc(List<ChapterNode> nodes, StringBuilder out, TocBuilder builder) {
        if (nodes == null) {
            return;
        }
        for (ChapterNode node : nodes) {
            int sectionNum = builder.next();
            String label = (node.title() != null && !node.title().isEmpty())
                    ? node.title()
                    : "Chapter " + sectionNum;
            String href = CONTENT_FILE + "#section-" + sectionNum;
            if (node.isContainer()) {
                out.append("        <li><a href=\"").append(XmlUtils.escapeXml(href)).append("\">")
                        .append(XmlUtils.escapeXml(label)).append("</a>\n");
                out.append("          <ol>\n");
                buildToc(node.children(), out, builder);
                out.append("          </ol>\n");
                out.append("        </li>\n");
            } else {
                out.append("        <li><a href=\"").append(XmlUtils.escapeXml(href)).append("\">")
                        .append(XmlUtils.escapeXml(label)).append("</a></li>\n");
            }
        }
    }

    private static class TocBuilder {
        private int next = 1;

        int next() {
            return next++;
        }

        int count() {
            return next - 1;
        }
    }
}
