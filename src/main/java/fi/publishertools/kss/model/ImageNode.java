package fi.publishertools.kss.model;

/**
 * Image reference from IDML Link element (inside CharacterStyleRange).
 * Has AppliedCharacterStyle from the parent CharacterStyleRange.
 */
public record ImageNode(String imageRef, String appliedCharacterStyle) implements ChapterNode {

    @Override
    public String imageRef() {
        return imageRef != null ? imageRef : "";
    }

    @Override
    public String appliedCharacterStyle() {
        return appliedCharacterStyle;
    }
}
