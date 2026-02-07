package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.A2_ExtractChapters;
import fi.publishertools.kss.util.XmlUtils;

class ExtractChaptersPhaseTest {

    @Test
    @DisplayName("ExtractChaptersPhase produces hierarchical ChapterNode from story XML with text")
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
        Document doc = XmlUtils.parseXml(storyXml.getBytes(StandardCharsets.UTF_8));
        ProcessingContext context = createContext(zipBytes, List.of(doc));

        A2_ExtractChapters phase = new A2_ExtractChapters();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(1);
        ChapterNode storyNode = chapters.get(0);
        assertThat(storyNode).isInstanceOf(StoryNode.class);
        assertThat(storyNode.isContainer()).isTrue();
        assertThat(storyNode.children()).hasSize(2);

        ChapterNode psr1 = storyNode.children().get(0);
        assertThat(psr1).isInstanceOf(ParagraphStyleRangeNode.class);
        assertThat(psr1.isContainer()).isTrue();
        assertThat(psr1.children()).hasSize(1);
        assertThat(psr1.children().get(0).isText()).isTrue();
        assertThat(psr1.children().get(0).text()).isEqualTo("Hello world");

        ChapterNode psr2 = storyNode.children().get(1);
        assertThat(psr2.isContainer()).isTrue();
        assertThat(psr2.children()).hasSize(1);
        assertThat(psr2.children().get(0).isText()).isTrue();
        assertThat(psr2.children().get(0).text()).isEqualTo("Second paragraph");
    }

    @Test
    @DisplayName("ExtractChaptersPhase produces empty list when no stories")
    void emptyWhenNoStories() throws Exception {
        byte[] zipBytes = new byte[0];
        ProcessingContext context = createContext(zipBytes, null);

        A2_ExtractChapters phase = new A2_ExtractChapters();
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
        Document doc = XmlUtils.parseXml(storyXml.getBytes(StandardCharsets.UTF_8));
        ProcessingContext context = createContext(zipBytes, List.of(doc));

        A2_ExtractChapters phase = new A2_ExtractChapters();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(1);
        ChapterNode storyNode = chapters.get(0);
        assertThat(storyNode.children()).hasSize(3);

        ChapterNode psr1 = storyNode.children().get(0);
        assertThat(psr1.children().get(0).isText()).isTrue();
        assertThat(psr1.children().get(0).text()).isEqualTo("Before image");

        ChapterNode psr2 = storyNode.children().get(1);
        assertThat(psr2.children().get(0).isImage()).isTrue();
        assertThat(psr2.children().get(0).imageRef()).isEqualTo("photo.jpg");

        ChapterNode psr3 = storyNode.children().get(2);
        assertThat(psr3.children().get(0).isText()).isTrue();
        assertThat(psr3.children().get(0).text()).isEqualTo("After image");
    }

    @Test
    @DisplayName("ExtractChaptersPhase stores style attributes on nodes")
    void storesStyleAttributes() throws Exception {
        String storyXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root>
              <Story AppliedTOCStyle="TOCStyle/Chapter">
                <ParagraphStyleRange AppliedParagraphStyle="ParagraphStyle/Heading1">
                  <CharacterStyleRange AppliedCharacterStyle="CharacterStyle/Bold">
                    <Content>Styled text</Content>
                  </CharacterStyleRange>
                </ParagraphStyleRange>
              </Story>
            </Root>
            """;

        byte[] zipBytes = createZipWithStory("Stories/Story_u123.xml", storyXml);
        Document doc = XmlUtils.parseXml(storyXml.getBytes(StandardCharsets.UTF_8));
        ProcessingContext context = createContext(zipBytes, List.of(doc));

        A2_ExtractChapters phase = new A2_ExtractChapters();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(1);
        ChapterNode storyNode = chapters.get(0);
        assertThat(storyNode.appliedStyle()).isEqualTo("TOCStyle/Chapter");

        ChapterNode psr = storyNode.children().get(0);
        assertThat(psr.appliedStyle()).isEqualTo("ParagraphStyle/Heading1");

        ChapterNode textNode = psr.children().get(0);
        assertThat(textNode.appliedStyle()).isEqualTo("CharacterStyle/Bold");
        assertThat(textNode.text()).isEqualTo("Styled text");
    }

    @Test
    @DisplayName("ExtractChaptersPhase handles multiple CharacterStyleRanges as siblings under ParagraphStyleRange")
    void multipleCharacterStyleRangesAsSiblings() throws Exception {
        String storyXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root>
              <Story>
                <ParagraphStyleRange>
                  <CharacterStyleRange AppliedCharacterStyle="CharacterStyle/Italic">
                    <Content>Italic part</Content>
                  </CharacterStyleRange>
                  <CharacterStyleRange AppliedCharacterStyle="CharacterStyle/$ID/[No character style]">
                    <Content> and normal part</Content>
                  </CharacterStyleRange>
                </ParagraphStyleRange>
              </Story>
            </Root>
            """;

        byte[] zipBytes = createZipWithStory("Stories/Story_u123.xml", storyXml);
        Document doc = XmlUtils.parseXml(storyXml.getBytes(StandardCharsets.UTF_8));
        ProcessingContext context = createContext(zipBytes, List.of(doc));

        A2_ExtractChapters phase = new A2_ExtractChapters();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        ChapterNode psr = chapters.get(0).children().get(0);
        assertThat(psr.children()).hasSize(2);
        assertThat(psr.children().get(0).text()).isEqualTo("Italic part");
        assertThat(psr.children().get(0).appliedStyle()).isEqualTo("CharacterStyle/Italic");
        assertThat(psr.children().get(1).text()).isEqualTo(" and normal part");
        assertThat(psr.children().get(1).appliedStyle()).isEqualTo("CharacterStyle/$ID/[No character style]");
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

    private static ProcessingContext createContext(byte[] zipBytes, List<Document> storyDocs) {
        StoredFile storedFile = new StoredFile(
                "test-id",
                "test.idml",
                "application/zip",
                (long) zipBytes.length,
                java.time.Instant.now(),
                zipBytes
        );
        ProcessingContext context = new ProcessingContext(storedFile);
        context.setStoriesList(storyDocs);
        return context;
    }
}
