package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.AssembleEpubPhase;

class AssembleEpubPhaseTest {

    @Test
    @DisplayName("AssembleEpubPhase creates EPUB ZIP with correct first two entries")
    void assembleEpubPhaseCreatesExpectedZip() throws Exception {
        StoredFile storedFile = new StoredFile(
                "file-id",
                "example.idml",
                "application/zip",
                0L,
                java.time.Instant.now(),
                new byte[0]
        );
        ProcessingContext context = new ProcessingContext(storedFile);

        AssembleEpubPhase phase = new AssembleEpubPhase();
        phase.process(context);

        byte[] epubBytes = context.getMetadata("epubFile", byte[].class);
        assertThat(epubBytes).as("epubFile metadata").isNotNull();
        assertThat(epubBytes.length).as("epubFile length").isGreaterThan(0);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(epubBytes))) {
            ZipEntry first = zis.getNextEntry();
            assertThat(first).as("first entry").isNotNull();
            assertThat(first.getName()).isEqualTo("mimetype");
            String mimetypeContent = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(mimetypeContent).isEqualTo("application/epub+zip");

            ZipEntry second = zis.getNextEntry();
            assertThat(second).as("second entry").isNotNull();
            assertThat(second.getName()).isEqualTo("META-INF/container.xml");
            byte[] secondContent = readAllBytes(zis);
            assertThat(secondContent.length).as("container.xml content").isGreaterThan(0);
            String containerXml = new String(secondContent, java.nio.charset.StandardCharsets.UTF_8);
            assertThat(containerXml).contains("OEBPS/contents.opf");
        }
    }

    @Test
    @DisplayName("AssembleEpubPhase adds images from imageContent to OEBPS/images/")
    void assembleEpubPhaseAddsImagesToZip() throws Exception {
        StoredFile storedFile = new StoredFile(
                "file-id",
                "example.idml",
                "application/zip",
                0L,
                java.time.Instant.now(),
                new byte[0]
        );
        ProcessingContext context = new ProcessingContext(storedFile);
        context.addMetadata("identifier", "test-id");
        context.addMetadata("title", "Test");
        context.addMetadata("creator", "Author");
        context.addMetadata("publisher", "Publisher");
        context.addMetadata("language", "fi");
        context.setPackageObf("<package/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        context.setXhtmlContent("<html/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        context.setTocContent("<nav/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        byte[] pngContent = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };
        context.addImageContent("cover.png", pngContent);
        context.addImageContent("photo.jpg", new byte[] { (byte) 0xFF, (byte) 0xD8 });

        AssembleEpubPhase phase = new AssembleEpubPhase();
        phase.process(context);

        byte[] epubBytes = context.getMetadata("epubFile", byte[].class);
        assertThat(epubBytes).as("epubFile metadata").isNotNull();

        java.util.Map<String, byte[]> entries = readZipEntries(epubBytes);
        assertThat(entries).containsKey("OEBPS/images/cover.png");
        assertThat(entries).containsKey("OEBPS/images/photo.jpg");
        assertThat(entries.get("OEBPS/images/cover.png")).isEqualTo(pngContent);
        assertThat(entries.get("OEBPS/images/photo.jpg")).isEqualTo(new byte[] { (byte) 0xFF, (byte) 0xD8 });
    }

    private static java.util.Map<String, byte[]> readZipEntries(byte[] zipBytes) throws IOException {
        java.util.Map<String, byte[]> result = new java.util.HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                result.put(entry.getName(), zis.readAllBytes());
            }
        }
        return result;
    }

    private static byte[] readAllBytes(ZipInputStream zis) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            while ((read = zis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }
}

