package fi.publishertools.kss.model.content;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IDML CharacterStyleRange element: text leaf with AppliedCharacterStyle.
 * Same concept as a text paragraph.
 * Optional language is a BCP 47 code for non-main-language segments (null = main language).
 */
public record CharacterStyleRangeNode(
        @JsonProperty("text") String text,
        @JsonProperty("appliedStyle") String appliedStyle,
        @JsonProperty("language") String language) implements ChapterNode {

    @Override
    public String text() {
        return text != null ? text : "";
    }
}
