package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ChapterNode;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.ExtractChaptersPhase;

class ExtractChaptersPhaseTest {

    @Test
    @DisplayName("ExtractChaptersPhase produces ChapterNode list from story XML with text")
    void extractsTextAsChapterNodes() throws Exception {
        String storyXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root>
              <Story>
                <ParagraphStyleRange>
                  <CharacterStyleRange>
                    <Content>Hello world</Content>
                  </CharacterStyleRange>
                </ParagraphStyleRange>
                <ParagraphStyleRange>
                  <CharacterStyleRange>
                    <Content>Second paragraph</Content>
                  </CharacterStyleRange>
                </ParagraphStyleRange>
              </Story>
            </Root>
            """;

        byte[] zipBytes = createZipWithStory("Stories/Story_u123.xml", storyXml);
        ProcessingContext context = createContext(zipBytes, List.of("Stories/Story_u123.xml"));

        ExtractChaptersPhase phase = new ExtractChaptersPhase();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(2);
        assertThat(chapters.get(0).isText()).isTrue();
        assertThat(chapters.get(0).text()).isEqualTo("Hello world");
        assertThat(chapters.get(1).isText()).isTrue();
        assertThat(chapters.get(1).text()).isEqualTo("Second paragraph");
    }

    @Test
    @DisplayName("ExtractChaptersPhase produces empty list when no stories")
    void emptyWhenNoStories() throws Exception {
        byte[] zipBytes = new byte[0];
        ProcessingContext context = createContext(zipBytes, null);

        ExtractChaptersPhase phase = new ExtractChaptersPhase();
        phase.process(context);

        assertThat(context.getChapters()).isEmpty();
    }

    @Test
    @DisplayName("ExtractChaptersPhase extracts image refs from Link elements in document order")
    void extractsTextAndImagesInOrder() throws Exception {
        String storyXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root>
              <Story>
                <ParagraphStyleRange>
                  <CharacterStyleRange>
                    <Content>Before image</Content>
                  </CharacterStyleRange>
                </ParagraphStyleRange>
                <ParagraphStyleRange>
                  <CharacterStyleRange>
                    <Link LinkResourceURI="Resources/Graphic/photo.jpg" LinkResourceFormat="JPEG"/>
                  </CharacterStyleRange>
                </ParagraphStyleRange>
                <ParagraphStyleRange>
                  <CharacterStyleRange>
                    <Content>After image</Content>
                  </CharacterStyleRange>
                </ParagraphStyleRange>
              </Story>
            </Root>
            """;

        byte[] zipBytes = createZipWithStory("Stories/Story_u123.xml", storyXml);
        ProcessingContext context = createContext(zipBytes, List.of("Stories/Story_u123.xml"));

        ExtractChaptersPhase phase = new ExtractChaptersPhase();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(3);
        assertThat(chapters.get(0).isText()).isTrue();
        assertThat(chapters.get(0).text()).isEqualTo("Before image");
        assertThat(chapters.get(1).isImage()).isTrue();
        assertThat(chapters.get(1).imageRef()).isEqualTo("photo.jpg");
        assertThat(chapters.get(2).isText()).isTrue();
        assertThat(chapters.get(2).text()).isEqualTo("After image");
    }

    private static byte[] createZipWithStory(String entryName, String content) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private static ProcessingContext createContext(byte[] zipBytes, List<String> storiesList) {
        StoredFile storedFile = new StoredFile(
                "test-id",
                "test.idml",
                "application/zip",
                (long) zipBytes.length,
                java.time.Instant.now(),
                zipBytes
        );
        ProcessingContext context = new ProcessingContext(storedFile);
        context.setStoriesList(storiesList);
        return context;
    }
}
