package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.integration.ollama.OllamaClient;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.phases.B2_ProposeImageAltTexts;

class B2_ProposeImageAltTextsTest {

    private StubOllamaClient stubClient;
    private B2_ProposeImageAltTexts phase;

    @BeforeEach
    void setUp() {
        stubClient = new StubOllamaClient();
        phase = new B2_ProposeImageAltTexts(stubClient);
    }

    @Test
    @DisplayName("image with content and no alt text gets generated description")
    void fillsAlternateTextWhenMissing() {
        ProcessingContext context = contextWithImageList(
                new ImageNode("uri", "img.png", "PNG", null, null));
        context.addImageContent("img.png", new byte[] { 1, 2, 3 });
        stubClient.setResult("A red apple");

        phase.process(context);

        List<ImageNode> list = context.getImageList();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).alternateText()).isEqualTo("A red apple");
    }

    @Test
    @DisplayName("image with existing alt text is left unchanged")
    void keepsExistingAltText() {
        ProcessingContext context = contextWithImageList(
                new ImageNode("uri", "img.png", "PNG", null, "Existing description"));
        context.addImageContent("img.png", new byte[] { 1, 2, 3 });

        phase.process(context);

        List<ImageNode> list = context.getImageList();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).alternateText()).isEqualTo("Existing description");
        assertThat(stubClient.invocationCount()).isZero();
    }

    @Test
    @DisplayName("image without content is skipped without exception")
    void skipsWhenNoContent() {
        ProcessingContext context = contextWithImageList(
                new ImageNode("uri", "img.png", "PNG", null, null));
        // no context.addImageContent

        phase.process(context);

        List<ImageNode> list = context.getImageList();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).alternateText()).isNull();
        assertThat(stubClient.invocationCount()).isZero();
    }

    @Test
    @DisplayName("client failure logs and skips image but phase completes")
    void continuesOnOllamaFailure() {
        ProcessingContext context = contextWithImageList(
                new ImageNode("uri", "img.png", "PNG", null, null));
        context.addImageContent("img.png", new byte[] { 1, 2, 3 });
        stubClient.setThrowOnInvocation(true);

        phase.process(context);

        List<ImageNode> list = context.getImageList();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).alternateText()).isNull();
    }

    @Test
    @DisplayName("empty image list does nothing")
    void emptyListNoOp() {
        ProcessingContext context = contextWithImageList(List.of());
        phase.process(context);
        assertThat(context.getImageList()).isEmpty();
    }

    @Test
    @DisplayName("null image list does nothing")
    void nullListNoOp() {
        ProcessingContext context = new ProcessingContext(
                new StoredFile("f1", "x.idml", "application/zip", 0L, java.time.Instant.EPOCH, new byte[0]));
        context.setImageList(null);
        phase.process(context);
        assertThat(context.getImageList()).isNull();
    }

    @Test
    @DisplayName("alt text is also applied to ImageNodes in chapter tree")
    void altTextPropagatesToChapters() {
        ImageNode imageNode = new ImageNode("uri", "img.png", "PNG", null, null);
        ChapterNode paragraph = new ParagraphStyleRangeNode(List.of(imageNode), "ParagraphStyle/Body");
        StoryNode story = new StoryNode(List.of(paragraph), "TOCStyle/Chapter");

        ProcessingContext context = contextWithImageList(imageNode);
        context.setChapters(List.of(story));
        context.addImageContent("img.png", new byte[] { 1, 2, 3 });
        stubClient.setResult("Alt from Ollama");

        phase.process(context);

        // flat list updated
        assertThat(context.getImageList()).hasSize(1);
        assertThat(context.getImageList().get(0).alternateText()).isEqualTo("Alt from Ollama");

        // chapter tree node updated (same instance)
        ChapterNode chapterParagraph = context.getChapters().get(0).children().get(0);
        assertThat(chapterParagraph).isInstanceOf(ParagraphStyleRangeNode.class);
        ChapterNode chapterImage = chapterParagraph.children().get(0);
        assertThat(chapterImage).isInstanceOf(ImageNode.class);
        assertThat(((ImageNode) chapterImage).alternateText()).isEqualTo("Alt from Ollama");
    }

    private static ProcessingContext contextWithImageList(ImageNode... nodes) {
        return contextWithImageList(List.of(nodes));
    }

    private static ProcessingContext contextWithImageList(List<ImageNode> nodes) {
        ProcessingContext context = new ProcessingContext(
                new StoredFile("f1", "x.idml", "application/zip", 0L, java.time.Instant.EPOCH, new byte[0]));
        context.setImageList(nodes);
        return context;
    }

    private static final class StubOllamaClient extends OllamaClient {
        private Optional<String> result = Optional.empty();
        private boolean throwOnInvocation;
        private int invocationCount;

        void setResult(String description) {
            this.result = Optional.of(description);
        }

        void setThrowOnInvocation(boolean b) {
            this.throwOnInvocation = b;
        }

        int invocationCount() {
            return invocationCount;
        }

        @Override
        public Optional<String> describeImage(byte[] imageContent) {
            invocationCount++;
            if (throwOnInvocation) {
                throw new RuntimeException("simulated Ollama failure");
            }
            return result;
        }
    }
}
