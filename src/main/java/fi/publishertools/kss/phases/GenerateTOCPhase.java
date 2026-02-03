package fi.publishertools.kss.phases;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Generates XHTML table-of-contents content for the EPUB.
 * Uses chapters from the context to build a nav element with epub:type="toc"
 * and links to each chapter section in the main content.
 */
public class GenerateTOCPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(GenerateTOCPhase.class);

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";
    private static final String EPUB_NS = "http://www.idpf.org/2007/ops";
    private static final String CONTENT_FILE = "Koottu-1.xhtml";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Generating TOC for file {}", context.getFileId());

        List<String> chapters = context.getChapters();
        if (chapters == null) {
            chapters = Collections.emptyList();
        }

        StringBuilder ol = new StringBuilder();
        for (int i = 0; i < chapters.size(); i++) {
            int sectionNum = i + 1;
            String label = "Chapter " + sectionNum;
            String href = CONTENT_FILE + "#section-" + sectionNum;
            ol.append("        <li><a href=\"").append(escapeXml(href)).append("\">")
                    .append(escapeXml(label)).append("</a></li>\n");
        }

        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"" + XHTML_NS + "\" xmlns:epub=\"" + EPUB_NS + "\">\n"
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

        context.setTocContent(xhtml.getBytes("utf-8"));
        logger.debug("Generated TOC with {} entries for file {}", chapters.size(), context.getFileId());
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
