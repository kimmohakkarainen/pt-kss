package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ChapterNode;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.GenerateXHTMLPhase;

class GenerateXHTMLPhaseTest {

    @Test
    @DisplayName("GenerateXHTMLPhase renders text nodes as paragraphs in sections")
    void rendersTextNodes() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of(
                ChapterNode.text("First paragraph"),
                ChapterNode.text("Second paragraph")
        ));
        context.addMetadata("language", "fi");

        GenerateXHTMLPhase phase = new GenerateXHTMLPhase();
        phase.process(context);

        String xhtml = new String(context.getXhtmlContent(), StandardCharsets.UTF_8);
        assertThat(xhtml).contains("<section class=\"chapter\" id=\"section-1\">");
        assertThat(xhtml).contains("<p>First paragraph</p>");
        assertThat(xhtml).contains("<section class=\"chapter\" id=\"section-2\">");
        assertThat(xhtml).contains("<p>Second paragraph</p>");
        assertThat(xhtml).contains("lang=\"fi\"");
    }

    @Test
    @DisplayName("GenerateXHTMLPhase renders image nodes as figures")
    void rendersImageNodes() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of(ChapterNode.image("photo.jpg")));
        context.addMetadata("language", "en");

        GenerateXHTMLPhase phase = new GenerateXHTMLPhase();
        phase.process(context);

        String xhtml = new String(context.getXhtmlContent(), StandardCharsets.UTF_8);
        assertThat(xhtml).contains("<figure><img src=\"images/photo.jpg\" alt=\"\"/></figure>");
    }

    @Test
    @DisplayName("GenerateXHTMLPhase renders container nodes with title and nested children")
    void rendersContainerNodes() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of(
                ChapterNode.section("Chapter 1", List.of(
                        ChapterNode.text("Intro text"),
                        ChapterNode.image("cover.png")
                ))
        ));
        context.addMetadata("language", "en");

        GenerateXHTMLPhase phase = new GenerateXHTMLPhase();
        phase.process(context);

        String xhtml = new String(context.getXhtmlContent(), StandardCharsets.UTF_8);
        assertThat(xhtml).contains("<h2>Chapter 1</h2>");
        assertThat(xhtml).contains("<p>Intro text</p>");
        assertThat(xhtml).contains("images/cover.png");
    }

    @Test
    @DisplayName("GenerateXHTMLPhase handles empty chapters")
    void handlesEmptyChapters() throws Exception {
        ProcessingContext context = createContext();
        context.setChapters(List.of());
        context.addMetadata("language", "en");

        GenerateXHTMLPhase phase = new GenerateXHTMLPhase();
        phase.process(context);

        String xhtml = new String(context.getXhtmlContent(), StandardCharsets.UTF_8);
        assertThat(xhtml).contains("<body>");
        assertThat(xhtml).contains("</body>");
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
