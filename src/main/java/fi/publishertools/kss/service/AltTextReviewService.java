package fi.publishertools.kss.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;

/**
 * Helpers for alt text review: image nodes in document order, syncing alt text to the chapter tree,
 * and surrounding text for each image occurrence.
 */
@Service
public class AltTextReviewService {

    private static final int SURROUNDING_TEXT_MAX_LENGTH = 300;

    /**
     * Returns image nodes from the chapter tree in document order (depth-first).
     * Index i corresponds to context.getImageList().get(i) for the same occurrence.
     */
    public List<ImageNode> getImageNodesInDocumentOrder(List<ChapterNode> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return List.of();
        }
        List<ImageNode> out = new ArrayList<>();
        collectImageNodesInOrder(chapters, out);
        return out;
    }

    private static void collectImageNodesInOrder(List<ChapterNode> nodes, List<ImageNode> out) {
        if (nodes == null) {
            return;
        }
        for (ChapterNode node : nodes) {
            if (node instanceof ImageNode img) {
                out.add(img);
            } else if (node instanceof StoryNode story) {
                collectImageNodesInOrder(story.children(), out);
            } else if (node instanceof ParagraphStyleRangeNode para) {
                collectImageNodesInOrder(para.children(), out);
            }
        }
    }

    /**
     * Sets alternate text on the i-th image in the chapter tree (document order) to match the flat image list.
     * Call after updating context.getImageList().get(index).setAlternateText(text).
     */
    public void syncAltTextToTreeAt(ProcessingContext context, int index) {
        List<ImageNode> treeImages = getImageNodesInDocumentOrder(context.getChapters());
        if (index < 0 || index >= treeImages.size()) {
            return;
        }
        List<ImageNode> imageList = context.getImageList();
        if (imageList == null || index >= imageList.size()) {
            return;
        }
        String alt = imageList.get(index).alternateText();
        treeImages.get(index).setAlternateText(alt);
    }

    /**
     * Result of surrounding text for one image occurrence.
     */
    public record SurroundingText(String textBefore, String textAfter) {}

    /**
     * Returns text before and after the i-th image in document order (from the chapter tree).
     * Each side is limited to {@value #SURROUNDING_TEXT_MAX_LENGTH} characters.
     */
    public SurroundingText getSurroundingText(List<ChapterNode> chapters, int imageIndex) {
        if (chapters == null || imageIndex < 0) {
            return new SurroundingText("", "");
        }
        List<String> textChunks = new ArrayList<>();
        List<Integer> imageIndices = new ArrayList<>();
        flattenTextAndImageIndices(chapters, textChunks, imageIndices);
        if (imageIndex >= imageIndices.size()) {
            return new SurroundingText("", "");
        }
        int pos = imageIndices.get(imageIndex);
        String before = concatenateWithLimit(textChunks, 0, pos, SURROUNDING_TEXT_MAX_LENGTH);
        String after = concatenateWithLimit(textChunks, pos + 1, textChunks.size(), SURROUNDING_TEXT_MAX_LENGTH);
        return new SurroundingText(before, after);
    }

    private static void flattenTextAndImageIndices(List<ChapterNode> nodes,
                                                   List<String> textChunks,
                                                   List<Integer> imageIndices) {
        if (nodes == null) {
            return;
        }
        for (ChapterNode node : nodes) {
            if (node instanceof CharacterStyleRangeNode textNode) {
                String t = textNode.text();
                textChunks.add(t != null ? t : "");
            } else if (node instanceof ImageNode) {
                imageIndices.add(textChunks.size()); // number of text chunks before this image
            } else if (node instanceof StoryNode story) {
                flattenTextAndImageIndices(story.children(), textChunks, imageIndices);
            } else if (node instanceof ParagraphStyleRangeNode para) {
                flattenTextAndImageIndices(para.children(), textChunks, imageIndices);
            }
        }
    }

    private static String concatenateWithLimit(List<String> chunks, int from, int to, int maxLen) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to && i < chunks.size(); i++) {
            String s = chunks.get(i);
            if (s != null && !s.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(s.trim());
                if (sb.length() >= maxLen) {
                    break;
                }
            }
        }
        String result = sb.toString().trim();
        if (result.length() > maxLen) {
            result = result.substring(0, maxLen).trim();
        }
        return result;
    }

    /**
     * Validates that index is within bounds for context.getImageList().
     */
    public boolean isValidOccurrenceIndex(ProcessingContext context, int index) {
        List<ImageNode> list = context.getImageList();
        return list != null && index >= 0 && index < list.size();
    }
}
