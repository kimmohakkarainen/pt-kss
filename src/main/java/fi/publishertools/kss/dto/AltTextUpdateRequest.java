package fi.publishertools.kss.dto;

/**
 * Request body for updating alt text on an image occurrence.
 * Empty string clears the alt text.
 */
public class AltTextUpdateRequest {

    private String alternateText;

    public AltTextUpdateRequest() {
    }

    public String getAlternateText() {
        return alternateText;
    }

    public void setAlternateText(String alternateText) {
        this.alternateText = alternateText;
    }
}
