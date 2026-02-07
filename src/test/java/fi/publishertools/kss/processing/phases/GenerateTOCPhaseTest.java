package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.phases.C2_GenerateTableOfContents;

class GenerateTOCPhaseTest {

    @Test
    @DisplayName("GenerateTOCPhase builds flat TOC for leaf nodes")
    void buildsFlatToc() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of(
                ChapterNode.text("First"),
                ChapterNode.text("Second")
        ));
        context.addMetadata("language", "fi");

        C2_GenerateTableOfContents phase = new C2_GenerateTableOfContents();
        phase.process(context);

        String toc = new String(context.getTocContent(), StandardCharsets.UTF_8);
        assertThat(toc).contains("Koottu-1.xhtml#section-1");
        assertThat(toc).contains("Koottu-1.xhtml#section-2");
        assertThat(toc).contains(">Chapter 1<");
        assertThat(toc).contains(">Chapter 2<");
    }

    @Test
    @DisplayName("GenerateTOCPhase uses Chapter N fallback when section has no title")
    void usesChapterFallbackWhenNoTitle() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of(
                ChapterNode.sectionWithParagraphStyle(null, List.of(ChapterNode.text("Content")), "ParagraphStyle/Body")
        ));
        context.addMetadata("language", "en");

        C2_GenerateTableOfContents phase = new C2_GenerateTableOfContents();
        phase.process(context);

        String toc = new String(context.getTocContent(), StandardCharsets.UTF_8);
        assertThat(toc).contains(">Chapter 1<");
    }

    @Test
    @DisplayName("GenerateTOCPhase builds nested TOC for container with children")
    void buildsNestedToc() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of(
                ChapterNode.sectionWithParagraphStyle(null, List.of(
                        ChapterNode.text("A"),
                        ChapterNode.text("B")
                ), "ParagraphStyle/Body")
        ));
        context.addMetadata("language", "en");

        C2_GenerateTableOfContents phase = new C2_GenerateTableOfContents();
        phase.process(context);

        String toc = new String(context.getTocContent(), StandardCharsets.UTF_8);
        assertThat(toc).contains(">Chapter 1<");
        assertThat(toc).contains("<ol>");
        assertThat(toc).contains("section-1");
        assertThat(toc).contains("section-2");
        assertThat(toc).contains("section-3");
    }

    @Test
    @DisplayName("GenerateTOCPhase handles empty chapters")
    void handlesEmptyChapters() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of());
        context.addMetadata("language", "en");

        C2_GenerateTableOfContents phase = new C2_GenerateTableOfContents();
        phase.process(context);

        String toc = new String(context.getTocContent(), StandardCharsets.UTF_8);
        assertThat(toc).contains("<nav epub:type=\"toc\">");
    }

    private static ProcessingContext createContext() {
        StoredFile storedFile = new StoredFile(
                "test-id",
                "test.idml",
                "application/zip",
                0L,
                java.time.Instant.now(),
                new byte[0]
        );
        return new ProcessingContext(storedFile);
    }
}
