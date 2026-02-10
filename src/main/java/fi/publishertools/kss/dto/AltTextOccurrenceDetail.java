package fi.publishertools.kss.dto;

/**
 * Detail for one image occurrence (proposed alt text, current alt text, surrounding context).
 */
public class AltTextOccurrenceDetail {

    private final int index;
    private final String fileName;
    private final String proposedAltText;
    private final String alternateText;
    private final String textBefore;
    private final String textAfter;

    public AltTextOccurrenceDetail(int index, String fileName, String proposedAltText, String alternateText,
                                   String textBefore, String textAfter) {
        this.index = index;
        this.fileName = fileName;
        this.proposedAltText = proposedAltText;
        this.alternateText = alternateText;
        this.textBefore = textBefore;
        this.textAfter = textAfter;
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

    public String getTextBefore() {
        return textBefore;
    }

    public String getTextAfter() {
        return textAfter;
    }
}
