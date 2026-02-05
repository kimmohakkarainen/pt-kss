package fi.publishertools.kss.phases;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.container.ContainerXml;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Assembles a minimal EPUB-compatible ZIP archive in memory.
 * <p>
 * The archive contains:
 * <ul>
 *     <li>First entry: {@code mimetype}, stored (no compression) with content {@code application/epub+zip}</li>
 *     <li>Second entry: {@code META-INF/container.xml}, currently with empty content</li>
 * </ul>
 * The resulting ZIP bytes are stored into {@link ProcessingContext} metadata under key {@code "epubFile"}.
 */
public class AssembleEpubPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(AssembleEpubPhase.class);

    private static final String MIMETYPE_ENTRY_NAME = "mimetype";
    private static final String MIMETYPE_CONTENT = "application/epub+zip";
    private static final String CONTAINER_XML_PATH = "META-INF/container.xml";
    private static final String CONTENT_OPF_PATH = "OEBPS/contents.opf";
    private static final String XHTML_PATH = "OEBPS/Koottu-1.xhtml";
    private static final String TOC_PATH = "OEBPS/toc.xhtml";
    private static final String IMAGES_DIR = "OEBPS/images/";

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Assembling EPUB ZIP for file {}", context.getFileId());

        byte[] mimetypeBytes = MIMETYPE_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] containerBytes = ContainerXml.create(Arrays.asList(CONTENT_OPF_PATH));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            // Entry 1: mimetype (stored, no compression)
            ZipEntry mimetypeEntry = new ZipEntry(MIMETYPE_ENTRY_NAME);
            mimetypeEntry.setMethod(ZipEntry.STORED);

            CRC32 crc = new CRC32();
            crc.update(mimetypeBytes);
            long crcValue = crc.getValue();

            mimetypeEntry.setSize(mimetypeBytes.length);
            mimetypeEntry.setCompressedSize(mimetypeBytes.length);
            mimetypeEntry.setCrc(crcValue);

            zos.putNextEntry(mimetypeEntry);
            zos.write(mimetypeBytes);
            zos.closeEntry();

            // Entry 2: META-INF/container.xml (may use default compression)
            ZipEntry containerEntry = new ZipEntry(CONTAINER_XML_PATH);
            zos.putNextEntry(containerEntry);
            if (containerBytes.length > 0) {
                zos.write(containerBytes);
            }
            zos.closeEntry();

            
            // Entry 3: OEBPS/contents.opf (may use default compression)
            ZipEntry opfEntry = new ZipEntry(CONTENT_OPF_PATH);
            zos.putNextEntry(opfEntry);
            byte[] packageOpf = context.getPackageOpf();
            if (packageOpf != null && packageOpf.length > 0) {
                zos.write(packageOpf);
            }
            zos.closeEntry();

            // Entry 4: OEBPS/Koottu-1.xhtml (may use default compression)
            ZipEntry xhtmlEntry = new ZipEntry(XHTML_PATH);
            zos.putNextEntry(xhtmlEntry);
            byte[] xhtmlContent = context.getXhtmlContent();
            if (xhtmlContent != null && xhtmlContent.length > 0) {
                zos.write(xhtmlContent);
            }
            zos.closeEntry();

            // Entry 5: OEBPS/toc.xhtml (may use default compression)
            ZipEntry tocEntry = new ZipEntry(TOC_PATH);
            zos.putNextEntry(tocEntry);
            byte[] tocContent = context.getTocContent();
            if (tocContent != null && tocContent.length > 0) {
                zos.write(tocContent);
            }
            zos.closeEntry();

            // Entry 6+: OEBPS/images/{filename} for each image in imageContent
            Map<String, byte[]> imageContent = context.getImageContent();
            if (imageContent != null && !imageContent.isEmpty()) {
                for (Map.Entry<String, byte[]> entry : imageContent.entrySet()) {
                    String filename = entry.getKey();
                    byte[] imageBytes = entry.getValue();
                    if (filename != null && imageBytes != null && imageBytes.length > 0) {
                        ZipEntry imageEntry = new ZipEntry(IMAGES_DIR + filename);
                        zos.putNextEntry(imageEntry);
                        zos.write(imageBytes);
                        zos.closeEntry();
                    }
                }
            }

            zos.finish();

            byte[] epubBytes = baos.toByteArray();
            context.addMetadata("epubFile", epubBytes);

            logger.debug("Assembled EPUB ZIP ({} bytes) for file {}", epubBytes.length, context.getFileId());
        }
    }

}

