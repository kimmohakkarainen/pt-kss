package fi.publishertools.kss.model;

import java.util.List;

/**
 * Generic section (chapter/sub-chapter) with optional title.
 * Used for manual construction, e.g. ChapterNode.section("Chapter 1", children).
 */
public record SectionNode(String title, List<ChapterNode> children) implements ChapterNode {

    @Override
    public String title() {
        return title;
    }

    @Override
    public List<ChapterNode> children() {
        return children != null ? List.copyOf(children) : List.of();
    }
}
