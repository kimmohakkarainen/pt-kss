package fi.publishertools.kss.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IDML CharacterStyleRange element: text leaf with AppliedCharacterStyle.
 * Same concept as a text paragraph.
 */
public record CharacterStyleRangeNode(
        @JsonProperty("text") String text,
        @JsonProperty("appliedCharacterStyle") String appliedCharacterStyle) implements ChapterNode {

    @Override
    public String text() {
        return text != null ? text : "";
    }
}
