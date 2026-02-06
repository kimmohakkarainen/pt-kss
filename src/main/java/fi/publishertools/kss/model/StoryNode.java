package fi.publishertools.kss.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IDML Story element: container with AppliedTOCStyle.
 */
public record StoryNode(@JsonProperty("children") List<ChapterNode> children, @JsonProperty("appliedTOCStyle") String appliedTOCStyle) implements ChapterNode {

    @Override
    public List<ChapterNode> children() {
        return children != null ? List.copyOf(children) : List.of();
    }
}
