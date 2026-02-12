package fi.publishertools.kss.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detail for one language markup occurrence: phrase, proposed language, and surrounding text (textBefore, textAfter)")
public class LangMarkupOccurrenceDetail {

    private final int index;
    private final String phraseText;
    private final String proposedLanguage;
    private final String textBefore;
    private final String textAfter;

    public LangMarkupOccurrenceDetail(int index, String phraseText, String proposedLanguage,
                                     String textBefore, String textAfter) {
        this.index = index;
        this.phraseText = phraseText;
        this.proposedLanguage = proposedLanguage;
        this.textBefore = textBefore;
        this.textAfter = textAfter;
    }

    public int getIndex() {
        return index;
    }

    public String getPhraseText() {
        return phraseText;
    }

    public String getProposedLanguage() {
        return proposedLanguage;
    }

    public String getTextBefore() {
        return textBefore;
    }

    public String getTextAfter() {
        return textAfter;
    }
}
