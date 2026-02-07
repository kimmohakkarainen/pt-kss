package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.phases.A4_ResolveContentHierarchy;

class A4_ResolveContentHierarchyTest {

    @Test
    @DisplayName("A4_ResolveContentHierarchy passes through empty chapters")
    void emptyChaptersPassthrough() throws Exception {
        byte[] zipBytes = createZipWithStyles(createStylesXml());
        ProcessingContext context = createContext(zipBytes);
        context.setChapters(List.of());

        A4_ResolveContentHierarchy phase = new A4_ResolveContentHierarchy();
        phase.process(context);

        assertThat(context.getChapters()).isEmpty();
    }

    @Test
    @DisplayName("A4_ResolveContentHierarchy passes through when no Styles.xml")
    void passthroughWhenNoStylesXml() throws Exception {
        byte[] zipBytes = new byte[0];
        ProcessingContext context = createContext(zipBytes);
        ChapterNode h1 = ChapterNode.sectionWithParagraphStyle(null, List.of(ChapterNode.text("Chapter 1")), "ParagraphStyle/Heading1");
        ChapterNode body = ChapterNode.sectionWithParagraphStyle(null, List.of(ChapterNode.text("Content")), "ParagraphStyle/Body");
        StoryNode story = new StoryNode(List.of(h1, body), "TOCStyle/Chapter");
        context.setChapters(List.of(story));

        A4_ResolveContentHierarchy phase = new A4_ResolveContentHierarchy();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(1);
        assertThat(chapters.get(0)).isInstanceOf(StoryNode.class);
        assertThat(chapters.get(0).children()).hasSize(2);
    }

    @Test
    @DisplayName("A4_ResolveContentHierarchy reorganizes flat content into hierarchy based on Styles.xml")
    void reorganizesHierarchyFromStyles() throws Exception {
        String stylesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root>
              <ParagraphStyle Self="ParagraphStyle/Heading1" BasedOn="ParagraphStyle/[No paragraph style]"/>
              <ParagraphStyle Self="ParagraphStyle/Heading2" BasedOn="ParagraphStyle/Heading1"/>
              <ParagraphStyle Self="ParagraphStyle/Body" BasedOn="ParagraphStyle/Heading2"/>
            </Root>
            """;
        byte[] zipBytes = createZipWithStyles(stylesXml);

        ChapterNode h1 = ChapterNode.sectionWithParagraphStyle(null, List.of(ChapterNode.text("Chapter 1")), "ParagraphStyle/Heading1");
        ChapterNode body1 = ChapterNode.sectionWithParagraphStyle(null, List.of(ChapterNode.text("Intro text")), "ParagraphStyle/Body");
        ChapterNode h2 = ChapterNode.sectionWithParagraphStyle(null, List.of(ChapterNode.text("Section 1.1")), "ParagraphStyle/Heading2");
        ChapterNode body2 = ChapterNode.sectionWithParagraphStyle(null, List.of(ChapterNode.text("Section content")), "ParagraphStyle/Body");
        StoryNode story = new StoryNode(List.of(h1, body1, h2, body2), "TOCStyle/Chapter");

        ProcessingContext context = createContext(zipBytes);
        context.setChapters(List.of(story));

        A4_ResolveContentHierarchy phase = new A4_ResolveContentHierarchy();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(1);
        ChapterNode storyNode = chapters.get(0);
        assertThat(storyNode).isInstanceOf(StoryNode.class);
        assertThat(storyNode.children()).hasSize(1);

        ChapterNode topPsr = storyNode.children().get(0);
        assertThat(topPsr).isInstanceOf(ParagraphStyleRangeNode.class);
        assertThat(topPsr.appliedStyle()).isEqualTo("ParagraphStyle/Heading1");
        assertThat(topPsr.children()).hasSize(3);

        assertThat(topPsr.children().get(0).isText()).isTrue();
        assertThat(topPsr.children().get(0).text()).isEqualTo("Chapter 1");

        ChapterNode body1Node = topPsr.children().get(1);
        assertThat(body1Node).isInstanceOf(ParagraphStyleRangeNode.class);
        assertThat(body1Node.appliedStyle()).isEqualTo("ParagraphStyle/Body");
        assertThat(body1Node.children().get(0).text()).isEqualTo("Intro text");

        ChapterNode h2Node = topPsr.children().get(2);
        assertThat(h2Node).isInstanceOf(ParagraphStyleRangeNode.class);
        assertThat(h2Node.appliedStyle()).isEqualTo("ParagraphStyle/Heading2");
        assertThat(h2Node.children()).hasSize(2);
        assertThat(h2Node.children().get(0).text()).isEqualTo("Section 1.1");
        assertThat(h2Node.children().get(1).appliedStyle()).isEqualTo("ParagraphStyle/Body");
        assertThat(((ParagraphStyleRangeNode) h2Node.children().get(1)).children().get(0).text()).isEqualTo("Section content");
    }

    @Test
    @DisplayName("A4_ResolveContentHierarchy preserves non-StoryNode chapters")
    void preservesNonStoryChapters() throws Exception {
        byte[] zipBytes = createZipWithStyles(createStylesXml());
        ProcessingContext context = createContext(zipBytes);
        context.setChapters(List.of(
                ChapterNode.section("Manual Chapter", List.of(ChapterNode.text("Manual content")))
        ));

        A4_ResolveContentHierarchy phase = new A4_ResolveContentHierarchy();
        phase.process(context);

        List<ChapterNode> chapters = context.getChapters();
        assertThat(chapters).hasSize(1);
        assertThat(chapters.get(0).title()).isEqualTo("Manual Chapter");
        assertThat(chapters.get(0).children()).hasSize(1);
        assertThat(chapters.get(0).children().get(0).text()).isEqualTo("Manual content");
    }

    private static String createStylesXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Root>
              <ParagraphStyle Self="ParagraphStyle/Heading1" BasedOn="ParagraphStyle/[No paragraph style]"/>
              <ParagraphStyle Self="ParagraphStyle/Heading2" BasedOn="ParagraphStyle/Heading1"/>
            </Root>
            """;
    }

    private static byte[] createZipWithStyles(String stylesXml) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            ZipEntry entry = new ZipEntry("Resources/Styles.xml");
            zos.putNextEntry(entry);
            zos.write(stylesXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private static ProcessingContext createContext(byte[] zipBytes) {
        StoredFile storedFile = new StoredFile(
                "test-id",
                "test.idml",
                "application/zip",
                (long) zipBytes.length,
                java.time.Instant.now(),
                zipBytes
        );
        return new ProcessingContext(storedFile);
    }

}
