package fi.publishertools.kss.dto;

/**
 * Request body for updating metadata on a pending processing context.
 * All fields are optional; only provided fields are updated.
 */
public class PendingMetadataUpdateRequest {

    private String title;
    private String creator;
    private String publisher;
    private String language;
    private String identifier;

    public PendingMetadataUpdateRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
