package fi.publishertools.kss.model.content;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Recursive node representing book content from IDML: stories, paragraph style ranges,
 * character style ranges (text), image references, and generic sections.
 * <p>
 * Five variants:
 * <ul>
 *   <li><b>StoryNode</b> – IDML Story container with appliedStyle (TOC)</li>
 *   <li><b>ParagraphStyleRangeNode</b> – IDML ParagraphStyleRange container with appliedStyle (paragraph)</li>
 *   <li><b>CharacterStyleRangeNode</b> – IDML CharacterStyleRange text leaf with appliedStyle (character)</li>
 *   <li><b>ImageNode</b> – Image reference (from Link) with appliedStyle (character)</li>
 *   <li><b>SectionNode</b> – Generic section with optional title (for manual construction)</li>
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = StoryNode.class, name = "story"),
    @JsonSubTypes.Type(value = ParagraphStyleRangeNode.class, name = "paragraphStyleRange"),
    @JsonSubTypes.Type(value = CharacterStyleRangeNode.class, name = "characterStyleRange"),
    @JsonSubTypes.Type(value = ImageNode.class, name = "image"),
    @JsonSubTypes.Type(value = SectionNode.class, name = "section")
})
public sealed interface ChapterNode
        permits StoryNode, ParagraphStyleRangeNode, CharacterStyleRangeNode, ImageNode, SectionNode {

    @JsonIgnore
    default String title() {
        return null;
    }

    @JsonIgnore
    default List<ChapterNode> children() {
        return Collections.emptyList();
    }

    @JsonIgnore
    default String text() {
        return null;
    }

    @JsonIgnore
    default String imageRef() {
        return null;
    }

    default String appliedStyle() {
        return null;
    }

    @JsonIgnore
    default boolean isContainer() {
        return this instanceof StoryNode || this instanceof ParagraphStyleRangeNode || this instanceof SectionNode;
    }

    @JsonIgnore
    default boolean isText() {
        return text() != null;
    }

    @JsonIgnore
    default boolean isImage() {
        return imageRef() != null;
    }

    // Factory methods for backward compatibility and manual construction

    static ChapterNode text(String text) {
        return new CharacterStyleRangeNode(text != null ? text : "", null);
    }

    static ChapterNode text(String text, String appliedStyle) {
        return new CharacterStyleRangeNode(text != null ? text : "",
                appliedStyle != null && !appliedStyle.isEmpty() ? appliedStyle : null);
    }

    static ChapterNode image(String imageRef) {
        return new ImageNode(imageRef != null ? imageRef : "", null);
    }

    static ChapterNode image(String imageRef, String appliedStyle) {
        return new ImageNode(imageRef != null ? imageRef : "",
                appliedStyle != null && !appliedStyle.isEmpty() ? appliedStyle : null);
    }

    static ChapterNode section(String title, List<ChapterNode> children) {
        return new SectionNode(title, children != null ? List.copyOf(children) : Collections.emptyList());
    }

    static ChapterNode sectionWithTOCStyle(String title, List<ChapterNode> children, String appliedStyle) {
        return new StoryNode(children != null ? List.copyOf(children) : Collections.emptyList(),
                appliedStyle != null && !appliedStyle.isEmpty() ? appliedStyle : null);
    }

    static ChapterNode sectionWithParagraphStyle(String title, List<ChapterNode> children, String appliedStyle) {
        return new ParagraphStyleRangeNode(children != null ? List.copyOf(children) : Collections.emptyList(),
                appliedStyle != null && !appliedStyle.isEmpty() ? appliedStyle : null);
    }
}
