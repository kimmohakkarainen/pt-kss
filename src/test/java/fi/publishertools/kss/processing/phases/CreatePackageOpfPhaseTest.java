package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.C3_CreatePackageOpf;

class CreatePackageOpfPhaseTest {

    @Test @Disabled
    @DisplayName("CreatePackageOpfPhase creates minimal package.opf XML and stores it as metadata")
    void createPackageOpfCreatesMinimalXmlAndStoresAsMetadata() throws Exception {
        StoredFile storedFile = new StoredFile(
                "file-id-123",
                "example.idml",
                "application/zip",
                0L,
                java.time.Instant.now(),
                new byte[0]
        );
        ProcessingContext context = new ProcessingContext(storedFile);

        C3_CreatePackageOpf phase = new C3_CreatePackageOpf();
        phase.process(context);

        byte[] opfBytes = context.getMetadata("packageOpf", byte[].class);
        assertThat(opfBytes).as("packageOpf metadata").isNotNull();
        assertThat(opfBytes.length).as("packageOpf length").isGreaterThan(0);

        String xml = new String(opfBytes, StandardCharsets.UTF_8);
        assertThat(xml).as("XML should start with XML declaration")
                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<package");
        assertThat(xml).contains("xmlns=\"http://www.idpf.org/2007/opf\"");
        assertThat(xml).contains("version=\"3.0\"");
        assertThat(xml).contains("<metadata>");
        assertThat(xml).contains("<manifest>");
    }

    @Test
    @DisplayName("CreatePackageOpfPhase adds manifest items for images in imageContent")
    void createPackageOpfAddsImageManifestItems() throws Exception {
        StoredFile storedFile = new StoredFile(
                "file-id-123",
                "example.idml",
                "application/zip",
                0L,
                java.time.Instant.now(),
                new byte[0]
        );
        ProcessingContext context = new ProcessingContext(storedFile);
        context.addMetadata("identifier", "test-id");
        context.addMetadata("title", "Test Title");
        context.addMetadata("creator", "Test Author");
        context.addMetadata("publisher", "Test Publisher");
        context.addMetadata("language", "fi");
        context.addImageContent("cover.png", new byte[] { 1, 2, 3 });
        context.addImageContent("photo.jpg", new byte[] { 4, 5, 6 });

        C3_CreatePackageOpf phase = new C3_CreatePackageOpf();
        phase.process(context);

        byte[] opfBytes = context.getPackageOpf();
        assertThat(opfBytes).as("packageOpf").isNotNull();
        assertThat(opfBytes.length).as("packageOpf length").isGreaterThan(0);

        String xml = new String(opfBytes, StandardCharsets.UTF_8);
        assertThat(xml).contains("images/cover.png");
        assertThat(xml).contains("images/photo.jpg");
        assertThat(xml).contains("image/png");
        assertThat(xml).contains("image/jpeg");
        assertThat(xml).contains("id=\"img-0\"");
        assertThat(xml).contains("id=\"img-1\"");
    }
}

