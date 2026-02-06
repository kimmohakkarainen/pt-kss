package fi.publishertools.kss.model;

import java.util.List;

/**
 * IDML Story element: container with AppliedTOCStyle.
 */
public record StoryNode(List<ChapterNode> children, String appliedTOCStyle) implements ChapterNode {

    @Override
    public List<ChapterNode> children() {
        return children != null ? List.copyOf(children) : List.of();
    }

    @Override
    public String appliedTOCStyle() {
        return appliedTOCStyle;
    }
}
