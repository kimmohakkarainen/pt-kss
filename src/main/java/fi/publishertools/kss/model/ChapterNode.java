package fi.publishertools.kss.model;

import java.util.Collections;
import java.util.List;

/**
 * Recursive node representing book content: chapters, sub-chapters, paragraphs, and image references.
 * <p>
 * A node is either:
 * <ul>
 *   <li><b>Container</b> (chapter/sub-chapter): has {@code children}; {@code text} and {@code imageRef} are null</li>
 *   <li><b>Text leaf</b> (paragraph): has {@code text}; {@code children} null/empty, {@code imageRef} null</li>
 *   <li><b>Image leaf</b>: has {@code imageRef}; {@code children} null/empty, {@code text} null</li>
 * </ul>
 *
 * @param title    Optional title for TOC; typically used for container nodes
 * @param text     Paragraph text content; null for container or image nodes
 * @param imageRef Image filename (matches {@link ImageInfo#fileName()} / imageContent keys); null for text or container
 * @param children Sub-chapters or paragraphs; null or empty for leaf nodes
 */
public record ChapterNode(
        String title,
        String text,
        String imageRef,
        List<ChapterNode> children) {

    /**
     * Creates a text paragraph node.
     */
    public static ChapterNode text(String text) {
        return new ChapterNode(null, text != null ? text : "", null, null);
    }

    /**
     * Creates an image reference node.
     *
     * @param imageRef Filename matching imageContent keys
     */
    public static ChapterNode image(String imageRef) {
        return new ChapterNode(null, null, imageRef != null ? imageRef : "", null);
    }

    /**
     * Creates a section (chapter/sub-chapter) with children.
     *
     * @param title    Optional title for TOC
     * @param children Child nodes; must not be null
     */
    public static ChapterNode section(String title, List<ChapterNode> children) {
        return new ChapterNode(
                title,
                null,
                null,
                children != null ? List.copyOf(children) : Collections.emptyList());
    }

    /**
     * Returns true if this node is a container (has children).
     */
    public boolean isContainer() {
        return children != null && !children.isEmpty();
    }

    /**
     * Returns true if this node is a text paragraph.
     */
    public boolean isText() {
        return text != null;
    }

    /**
     * Returns true if this node is an image reference.
     */
    public boolean isImage() {
        return imageRef != null;
    }
}
