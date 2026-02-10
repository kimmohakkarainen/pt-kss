package fi.publishertools.kss.model.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Image reference from IDML Link element (inside CharacterStyleRange).
 * Holds resource URI, resolved filename, format, and AppliedCharacterStyle from the parent.
 * Used both as a node in the chapter tree and as an entry in the flat image list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageNode(
        String resourceUri,
        @JsonProperty("imageRef") String fileName,
        String resourceFormat,
        @JsonProperty("appliedStyle") String appliedStyle) implements ChapterNode {

    @Override
    public String imageRef() {
        return fileName != null ? fileName : "";
    }
}
