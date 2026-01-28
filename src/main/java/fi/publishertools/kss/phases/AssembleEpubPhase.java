package fi.publishertools.kss.phases;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
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

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Assembling EPUB ZIP for file {}", context.getFileId());

        byte[] mimetypeBytes = MIMETYPE_CONTENT.getBytes(StandardCharsets.UTF_8);
        byte[] containerBytes = new byte[0]; // empty for now

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

            zos.finish();

            byte[] epubBytes = baos.toByteArray();
            context.addMetadata("epubFile", epubBytes);

            logger.debug("Assembled EPUB ZIP ({} bytes) for file {}", epubBytes.length, context.getFileId());
        }
    }

}

