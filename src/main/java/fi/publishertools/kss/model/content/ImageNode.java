package fi.publishertools.kss.model.content;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Image reference from IDML Link element (inside CharacterStyleRange).
 * Has AppliedCharacterStyle from the parent CharacterStyleRange.
 */
public record ImageNode(@JsonProperty("imageRef") String imageRef,
        @JsonProperty("appliedStyle") String appliedStyle) implements ChapterNode {

    @Override
    public String imageRef() {
        return imageRef != null ? imageRef : "";
    }
}
