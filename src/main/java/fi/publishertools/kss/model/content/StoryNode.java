package fi.publishertools.kss.model.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IDML Story element: container with AppliedTOCStyle.
 */
public record StoryNode(@JsonProperty("children") List<ChapterNode> children, @JsonProperty("appliedStyle") String appliedStyle) implements ChapterNode {

    @Override
    public List<ChapterNode> children() {
        return children != null ? List.copyOf(children) : List.of();
    }
}
