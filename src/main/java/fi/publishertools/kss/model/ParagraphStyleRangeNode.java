package fi.publishertools.kss.model;

import java.util.List;

/**
 * IDML ParagraphStyleRange element: container with AppliedParagraphStyle.
 */
public record ParagraphStyleRangeNode(List<ChapterNode> children, String appliedParagraphStyle) implements ChapterNode {

    @Override
    public List<ChapterNode> children() {
        return children != null ? List.copyOf(children) : List.of();
    }

    @Override
    public String appliedParagraphStyle() {
        return appliedParagraphStyle;
    }
}
