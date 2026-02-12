package fi.publishertools.kss.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for PATCH lang-markup-review occurrence: optional language, phraseText, textBefore, textAfter, or dismiss.
 */
@Schema(description = "Update for one language markup occurrence. All fields optional. If dismiss is true, other fields are ignored.")
public class LangMarkupUpdateRequest {

    @Schema(description = "BCP 47 language code for the phrase (e.g. en, und)")
    private String language;

    @Schema(description = "Replacement text for the marked-up phrase")
    private String phraseText;

    @Schema(description = "Replacement text for the segment immediately before the phrase")
    private String textBefore;

    @Schema(description = "Replacement text for the segment immediately after the phrase")
    private String textAfter;

    @Schema(description = "If true, remove language markup by merging this segment into adjacent main-language text")
    private Boolean dismiss;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPhraseText() {
        return phraseText;
    }

    public void setPhraseText(String phraseText) {
        this.phraseText = phraseText;
    }

    public String getTextBefore() {
        return textBefore;
    }

    public void setTextBefore(String textBefore) {
        this.textBefore = textBefore;
    }

    public String getTextAfter() {
        return textAfter;
    }

    public void setTextAfter(String textAfter) {
        this.textAfter = textAfter;
    }

    public Boolean getDismiss() {
        return dismiss;
    }

    public void setDismiss(Boolean dismiss) {
        this.dismiss = dismiss;
    }
}
