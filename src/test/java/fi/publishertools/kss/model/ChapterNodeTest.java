package fi.publishertools.kss.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertThat(node.children()).isNull();
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
        assertThat(node.children()).isNull();
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
}
