package fi.publishertools.kss.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IDML ParagraphStyleRange element: container with AppliedParagraphStyle.
 */
public record ParagraphStyleRangeNode(@JsonProperty("children") List<ChapterNode> children, @JsonProperty("appliedParagraphStyle") String appliedParagraphStyle) implements ChapterNode {

    @Override
    public List<ChapterNode> children() {
        return children != null ? List.copyOf(children) : List.of();
    }
}
