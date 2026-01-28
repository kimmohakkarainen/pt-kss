package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.CreatePackageOpfPhase;

class CreatePackageOpfPhaseTest {

    @Test
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

        CreatePackageOpfPhase phase = new CreatePackageOpfPhase();
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
}

