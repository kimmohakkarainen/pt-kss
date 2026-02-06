package fi.publishertools.kss.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;

class ChapterNodeTest {

    @Test
    @DisplayName("ChapterNode.text creates text leaf")
    void textFactory() {
        ChapterNode node = ChapterNode.text("Hello");
        assertThat(node.isText()).isTrue();
        assertThat(node.isImage()).isFalse();
        assertThat(node.isContainer()).isFalse();
        assertThat(node.text()).isEqualTo("Hello");
        assertThat(node.imageRef()).isNull();
        assertThat(node.children()).isEmpty();
    }

    @Test
    @DisplayName("ChapterNode.image creates image leaf")
    void imageFactory() {
        ChapterNode node = ChapterNode.image("photo.jpg");
        assertThat(node.isImage()).isTrue();
        assertThat(node.isText()).isFalse();
        assertThat(node.isContainer()).isFalse();
        assertThat(node.imageRef()).isEqualTo("photo.jpg");
        assertThat(node.text()).isNull();
        assertThat(node.children()).isEmpty();
    }

    @Test
    @DisplayName("ChapterNode.section creates container with children")
    void sectionFactory() {
        ChapterNode child = ChapterNode.text("Content");
        ChapterNode node = ChapterNode.section("Chapter 1", List.of(child));
        assertThat(node.isContainer()).isTrue();
        assertThat(node.isText()).isFalse();
        assertThat(node.isImage()).isFalse();
        assertThat(node.title()).isEqualTo("Chapter 1");
        assertThat(node.children()).containsExactly(child);
    }

    @Test
    @DisplayName("ChapterNode.text with appliedCharacterStyle stores style")
    void textWithCharacterStyle() {
        ChapterNode node = ChapterNode.text("Hello", "CharacterStyle/Bold");
        assertThat(node.isText()).isTrue();
        assertThat(node.text()).isEqualTo("Hello");
        assertThat(node.appliedCharacterStyle()).isEqualTo("CharacterStyle/Bold");
    }

    @Test
    @DisplayName("ChapterNode.image with appliedCharacterStyle stores style")
    void imageWithCharacterStyle() {
        ChapterNode node = ChapterNode.image("photo.jpg", "CharacterStyle/Italic");
        assertThat(node.isImage()).isTrue();
        assertThat(node.imageRef()).isEqualTo("photo.jpg");
        assertThat(node.appliedCharacterStyle()).isEqualTo("CharacterStyle/Italic");
    }

    @Test
    @DisplayName("ChapterNode.sectionWithTOCStyle stores AppliedTOCStyle")
    void sectionWithTOCStyle() {
        ChapterNode child = ChapterNode.text("Content");
        ChapterNode node = ChapterNode.sectionWithTOCStyle(null, List.of(child), "TOCStyle/Chapter");
        assertThat(node.isContainer()).isTrue();
        assertThat(node.appliedTOCStyle()).isEqualTo("TOCStyle/Chapter");
        assertThat(node.children()).containsExactly(child);
    }

    @Test
    @DisplayName("ChapterNode.sectionWithParagraphStyle stores AppliedParagraphStyle")
    void sectionWithParagraphStyle() {
        ChapterNode child = ChapterNode.text("Content");
        ChapterNode node = ChapterNode.sectionWithParagraphStyle(null, List.of(child), "ParagraphStyle/Heading1");
        assertThat(node.isContainer()).isTrue();
        assertThat(node.appliedParagraphStyle()).isEqualTo("ParagraphStyle/Heading1");
        assertThat(node.children()).containsExactly(child);
    }

    @Test @Disabled
    @DisplayName("ChapterNode hierarchy serializes and deserializes to JSON with type info")
    void jsonRoundTrip() throws Exception {
        ChapterNode node = ChapterNode.sectionWithTOCStyle(null,
                List.of(ChapterNode.sectionWithParagraphStyle(null,
                        List.of(ChapterNode.text("Hello", "CharacterStyle/Bold")),
                        "ParagraphStyle/Heading1")),
                "TOCStyle/Chapter");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(node);
        assertThat(json).contains("\"type\":\"story\"");
        assertThat(json).contains("appliedTOCStyle");
        assertThat(json).contains("Hello");

        ChapterNode deserialized = mapper.readValue(json, ChapterNode.class);
        assertThat(deserialized).isInstanceOf(StoryNode.class);
        assertThat(deserialized.appliedTOCStyle()).isEqualTo("TOCStyle/Chapter");
        assertThat(deserialized.children()).hasSize(1);
        assertThat(deserialized.children().get(0)).isInstanceOf(ParagraphStyleRangeNode.class);
        assertThat(deserialized.children().get(0).children().get(0).text()).isEqualTo("Hello");
    }
}
