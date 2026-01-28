package fi.publishertools.kss.phases;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Creates a minimal EPUB 3 {@code package.opf} XML document and stores it
 * as UTF-8 bytes into {@link ProcessingContext} metadata under key {@code "packageOpf"}.
 * <p>
 * The document:
 * <ul>
 *     <li>Uses the {@code http://www.idpf.org/2007/opf} namespace</li>
 *     <li>Has root element {@code &lt;package&gt;} with version 3.0</li>
 *     <li>Contains minimal, mostly empty {@code &lt;metadata&gt;} and {@code &lt;manifest&gt;} elements</li>
 * </ul>
 */
public class CreatePackageOpfPhase implements ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(CreatePackageOpfPhase.class);

    private static final String OPF_NAMESPACE = "http://www.idpf.org/2007/opf";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Creating package.opf for file {}", context.getFileId());

        String fileId = context.getFileId();
        // Use a stable identifier derived from fileId for uniqueness inside the OPF
        String uniqueId = (fileId != null && !fileId.isEmpty()) ? fileId : "pub-id";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<package");
        sb.append(" xmlns=\"").append(OPF_NAMESPACE).append("\"");
        sb.append(" version=\"3.0\"");
        sb.append(" unique-identifier=\"pub-id\"");
        sb.append(">\n");

        // Minimal metadata section; can be enriched in later iterations
        sb.append("  <metadata>\n");
        sb.append("    <dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">")
          .append(escapeXml(context.getOriginalFilename()))
          .append("</dc:title>\n");
        sb.append("    <dc:identifier xmlns:dc=\"http://purl.org/dc/elements/1.1/\" id=\"pub-id\">")
          .append(escapeXml(uniqueId))
          .append("</dc:identifier>\n");
        sb.append("  </metadata>\n");

        // Minimal, empty manifest for now; items can be added later
        sb.append("  <manifest>\n");
        sb.append("  </manifest>\n");

        // Spine can be omitted in a stub; left out intentionally

        sb.append("</package>\n");

        byte[] opfBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        context.addMetadata("packageOpf", opfBytes);

        logger.debug("Created package.opf ({} bytes) for file {}", opfBytes.length, context.getFileId());
    }

    @Override
    public String getName() {
        return "CreatePackageOpf";
    }

    /**
     * Very small XML escape helper for text content.
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&apos;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}

