package fi.publishertools.kss.model.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Image reference from IDML Link element (inside CharacterStyleRange).
 * Holds resource URI, resolved filename, format, and AppliedCharacterStyle from the parent.
 * Used both as a node in the chapter tree and as an entry in the flat image list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ImageNode implements ChapterNode {

    private String resourceUri;

    @JsonProperty("imageRef")
    private String fileName;

    private String resourceFormat;

    @JsonProperty("appliedStyle")
    private String appliedStyle;

    @JsonProperty("alternateText")
    private String alternateText;

    public ImageNode(String resourceUri,
                     String fileName,
                     String resourceFormat,
                     String appliedStyle,
                     String alternateText) {
        this.resourceUri = resourceUri;
        this.fileName = fileName;
        this.resourceFormat = resourceFormat;
        this.appliedStyle = appliedStyle;
        this.alternateText = alternateText;
    }

    public String resourceUri() {
        return resourceUri;
    }

    public String fileName() {
        return fileName;
    }

    public String resourceFormat() {
        return resourceFormat;
    }

    @Override
    public String appliedStyle() {
        return appliedStyle;
    }

    public String alternateText() {
        return alternateText;
    }

    public void setAlternateText(String alternateText) {
        this.alternateText = alternateText;
    }

    @Override
    public String imageRef() {
        return fileName != null ? fileName : "";
    }
}
