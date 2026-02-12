package fi.publishertools.kss.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of one language markup occurrence (index, phrase text, proposed language)")
public class LangMarkupOccurrenceSummary {

    private final int index;
    private final String phraseText;
    private final String proposedLanguage;

    public LangMarkupOccurrenceSummary(int index, String phraseText, String proposedLanguage) {
        this.index = index;
        this.phraseText = phraseText;
        this.proposedLanguage = proposedLanguage;
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
}
