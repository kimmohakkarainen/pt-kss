package fi.publishertools.kss.dto;

/**
 * Summary of one image occurrence for the list endpoint.
 */
public class AltTextOccurrenceSummary {

    private final int index;
    private final String fileName;
    private final String proposedAltText;
    private final String alternateText;

    public AltTextOccurrenceSummary(int index, String fileName, String proposedAltText, String alternateText) {
        this.index = index;
        this.fileName = fileName;
        this.proposedAltText = proposedAltText;
        this.alternateText = alternateText;
    }

    public int getIndex() {
        return index;
    }

    public String getFileName() {
        return fileName;
    }

    public String getProposedAltText() {
        return proposedAltText;
    }

    public String getAlternateText() {
        return alternateText;
    }
}
