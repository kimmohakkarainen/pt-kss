package fi.publishertools.kss.phases;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Takes the chapters from the context and generates a single XHTML document.
 * The document has a head with title set to the original filename, and a body
 * where each chapter string is wrapped in {@code <section class="chapter">}.
 */
public class GenerateXHTMLPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(GenerateXHTMLPhase.class);

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Generating XHTML for file {}", context.getFileId());

        List<String> chapters = context.getChapters();
        if (chapters == null) {
            chapters = Collections.emptyList();
        }

        String title = context.getOriginalFilename() != null ? context.getOriginalFilename() : "";
        String escapedTitle = escapeXml(title);

        StringBuilder body = new StringBuilder();
        int sectionIndex = 1;
        for (String chapter : chapters) {
            String escaped = escapeXml(chapter != null ? chapter : "");
            body.append("    <section class=\"chapter\" id=\"section-").append(sectionIndex).append("\">")
                    .append(escaped).append("</section>\n");
            sectionIndex++;
        }

        String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"" + XHTML_NS + "\">\n"
                + "  <head>\n"
                + "    <meta charset=\"UTF-8\"/>\n"
                + "    <title>" + escapedTitle + "</title>\n"
                + "  </head>\n"
                + "  <body>\n"
                + body.toString()
                + "  </body>\n"
                + "</html>";

        context.setXhtmlContent(xhtml.getBytes("utf-8"));
        logger.debug("Generated XHTML with {} chapters for file {}", chapters.size(), context.getFileId());
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
