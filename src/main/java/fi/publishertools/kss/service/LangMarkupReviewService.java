package fi.publishertools.kss.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import fi.publishertools.kss.dto.LangMarkupOccurrenceDetail;
import fi.publishertools.kss.dto.LangMarkupOccurrenceSummary;
import fi.publishertools.kss.dto.LangMarkupUpdateRequest;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;

/**
 * Lists language markup occurrences, provides surrounding text, and applies user updates
 * (change language, edit phrase/surrounding text, or dismiss markup).
 */
@Service
public class LangMarkupReviewService {

    private static final int SURROUNDING_TEXT_MAX_LENGTH = 300;

    /**
     * One occurrence: path from root to the lang node (indices), and the node.
     */
    private record Occurrence(List<Integer> path, CharacterStyleRangeNode node) {}

    /**
     * Returns occurrences (index, phraseText, proposedLanguage) in document order.
     */
    public List<LangMarkupOccurrenceSummary> getOccurrenceSummaries(List<ChapterNode> chapters) {
        List<Occurrence> occurrences = collectOccurrences(chapters);
        List<LangMarkupOccurrenceSummary> result = new ArrayList<>();
        for (int i = 0; i < occurrences.size(); i++) {
            Occurrence occ = occurrences.get(i);
            String lang = occ.node().language();
            result.add(new LangMarkupOccurrenceSummary(
                    i,
                    occ.node().text() != null ? occ.node().text() : "",
                    lang != null ? lang : ""
            ));
        }
        return result;
    }

    /**
     * Returns detail for the occurrence at the given index: phraseText, proposedLanguage, textBefore, textAfter.
     */
    public LangMarkupOccurrenceDetail getOccurrenceDetail(List<ChapterNode> chapters, int occurrenceIndex) {
        List<Occurrence> occurrences = collectOccurrences(chapters);
        if (occurrenceIndex < 0 || occurrenceIndex >= occurrences.size()) {
            return null;
        }
        Occurrence occ = occurrences.get(occurrenceIndex);
        Surrounding surrounding = getSurroundingForOccurrence(chapters, occurrenceIndex);
        String lang = occ.node().language();
        return new LangMarkupOccurrenceDetail(
                occurrenceIndex,
                occ.node().text() != null ? occ.node().text() : "",
                lang != null ? lang : "",
                surrounding.textBefore(),
                surrounding.textAfter()
        );
    }

    /**
     * Applies the update to the occurrence at the given index and updates context.getChapters().
     * Returns the updated occurrence summary, or null if occurrence index invalid.
     */
    public LangMarkupOccurrenceSummary applyUpdate(ProcessingContext context, int occurrenceIndex,
                                                   LangMarkupUpdateRequest request) {
        List<ChapterNode> chapters = context.getChapters();
        if (chapters == null) {
            return null;
        }
        List<Occurrence> occurrences = collectOccurrences(chapters);
        if (occurrenceIndex < 0 || occurrenceIndex >= occurrences.size()) {
            return null;
        }
        Occurrence occ = occurrences.get(occurrenceIndex);
        List<Integer> path = occ.path();
        if (path.isEmpty()) {
            return null;
        }
        List<Integer> parentPath = path.subList(0, path.size() - 1);
        int indexInParent = path.get(path.size() - 1);
        ChapterNode parent = getNodeAt(chapters, parentPath);
        if (parent == null || !(parent instanceof StoryNode || parent instanceof ParagraphStyleRangeNode)) {
            return null;
        }
        List<ChapterNode> siblingList = parent.children();
        if (indexInParent < 0 || indexInParent >= siblingList.size()) {
            return null;
        }

        List<ChapterNode> newChildren = applyReplacement(siblingList, indexInParent, occ.node(), request);
        if (newChildren == null) {
            return null;
        }
        ChapterNode newParent = cloneWithChildren(parent, newChildren);
        List<ChapterNode> newChapters = replaceNodeAt(chapters, parentPath, 0, newParent);
        context.setChapters(newChapters);

        // Return updated summary for the same occurrence index (might be same or merged)
        List<Occurrence> afterOccurrences = collectOccurrences(newChapters);
        if (occurrenceIndex < afterOccurrences.size()) {
            Occurrence updated = afterOccurrences.get(occurrenceIndex);
            String lang = updated.node().language();
            return new LangMarkupOccurrenceSummary(
                    occurrenceIndex,
                    updated.node().text() != null ? updated.node().text() : "",
                    lang != null ? lang : ""
            );
        }
        return null;
    }

    public boolean isValidOccurrenceIndex(ProcessingContext context, int index) {
        List<Occurrence> occurrences = collectOccurrences(context.getChapters());
        return index >= 0 && index < occurrences.size();
    }

    private List<Occurrence> collectOccurrences(List<ChapterNode> chapters) {
        List<Occurrence> out = new ArrayList<>();
        collectOccurrences(chapters, new ArrayList<>(), out);
        return out;
    }

    private void collectOccurrences(List<ChapterNode> nodes, List<Integer> path, List<Occurrence> out) {
        if (nodes == null) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            ChapterNode node = nodes.get(i);
            List<Integer> nodePath = new ArrayList<>(path);
            nodePath.add(i);
            if (node instanceof CharacterStyleRangeNode csr) {
                if (csr.language() != null && !csr.language().isBlank()) {
                    out.add(new Occurrence(nodePath, csr));
                }
            } else if (node instanceof StoryNode story) {
                collectOccurrences(story.children(), nodePath, out);
            } else if (node instanceof ParagraphStyleRangeNode para) {
                collectOccurrences(para.children(), nodePath, out);
            }
        }
    }

    private record Surrounding(String textBefore, String textAfter) {}

    private Surrounding getSurroundingForOccurrence(List<ChapterNode> chapters, int occurrenceIndex) {
        List<String> textChunks = new ArrayList<>();
        List<Boolean> isLangChunk = new ArrayList<>();
        flattenTextAndLang(chapters, textChunks, isLangChunk);
        int langCount = 0;
        int pos = -1;
        for (int i = 0; i < isLangChunk.size(); i++) {
            if (Boolean.TRUE.equals(isLangChunk.get(i))) {
                if (langCount == occurrenceIndex) {
                    pos = i;
                    break;
                }
                langCount++;
            }
        }
        if (pos < 0) {
            return new Surrounding("", "");
        }
        String before = concatenateWithLimit(textChunks, 0, pos, SURROUNDING_TEXT_MAX_LENGTH);
        String after = concatenateWithLimit(textChunks, pos + 1, textChunks.size(), SURROUNDING_TEXT_MAX_LENGTH);
        return new Surrounding(before, after);
    }

    private void flattenTextAndLang(List<ChapterNode> nodes, List<String> textChunks, List<Boolean> isLangChunk) {
        if (nodes == null) {
            return;
        }
        for (ChapterNode node : nodes) {
            if (node instanceof CharacterStyleRangeNode csr) {
                String t = csr.text();
                textChunks.add(t != null ? t : "");
                isLangChunk.add(csr.language() != null && !csr.language().isBlank());
            } else if (node instanceof StoryNode story) {
                flattenTextAndLang(story.children(), textChunks, isLangChunk);
            } else if (node instanceof ParagraphStyleRangeNode para) {
                flattenTextAndLang(para.children(), textChunks, isLangChunk);
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

    private List<ChapterNode> applyReplacement(List<ChapterNode> siblingList, int index,
                                               CharacterStyleRangeNode langNode, LangMarkupUpdateRequest request) {
        if (request.getDismiss() != null && Boolean.TRUE.equals(request.getDismiss())) {
            return applyDismiss(siblingList, index, langNode);
        }
        String newLang = request.getLanguage() != null ? request.getLanguage() : langNode.language();
        String newPhrase = request.getPhraseText() != null ? request.getPhraseText() : langNode.text();
        String style = langNode.appliedStyle();
        CharacterStyleRangeNode newNode = new CharacterStyleRangeNode(
                newPhrase != null ? newPhrase : "",
                style,
                (newLang != null && !newLang.isBlank()) ? newLang : null
        );
        List<ChapterNode> result = new ArrayList<>(siblingList);
        result.set(index, newNode);

        if (request.getTextBefore() != null || request.getTextAfter() != null) {
            if (request.getTextBefore() != null && index > 0 && siblingList.get(index - 1) instanceof CharacterStyleRangeNode prev) {
                result.set(index - 1, new CharacterStyleRangeNode(request.getTextBefore(), prev.appliedStyle(), prev.language()));
            }
            if (request.getTextAfter() != null && index < siblingList.size() - 1 && siblingList.get(index + 1) instanceof CharacterStyleRangeNode next) {
                result.set(index + 1, new CharacterStyleRangeNode(request.getTextAfter(), next.appliedStyle(), next.language()));
            }
        }
        return result;
    }

    private List<ChapterNode> applyDismiss(List<ChapterNode> siblingList, int index, CharacterStyleRangeNode langNode) {
        CharacterStyleRangeNode prev = index > 0 && siblingList.get(index - 1) instanceof CharacterStyleRangeNode p ? p : null;
        CharacterStyleRangeNode next = index < siblingList.size() - 1 && siblingList.get(index + 1) instanceof CharacterStyleRangeNode n ? n : null;
        String mergedText = langNode.text() != null ? langNode.text() : "";
        List<ChapterNode> result = new ArrayList<>();
        if (prev != null) {
            for (int i = 0; i < index - 1; i++) {
                result.add(siblingList.get(i));
            }
            String prevText = prev.text() != null ? prev.text() : "";
            result.add(new CharacterStyleRangeNode(prevText + mergedText, prev.appliedStyle(), null));
            for (int i = index + 1; i < siblingList.size(); i++) {
                result.add(siblingList.get(i));
            }
        } else if (next != null) {
            for (int i = 0; i < index; i++) {
                result.add(siblingList.get(i));
            }
            String nextText = next.text() != null ? next.text() : "";
            result.add(new CharacterStyleRangeNode(mergedText + nextText, next.appliedStyle(), null));
            for (int i = index + 2; i < siblingList.size(); i++) {
                result.add(siblingList.get(i));
            }
        } else {
            result.addAll(siblingList);
            result.set(index, new CharacterStyleRangeNode(langNode.text(), langNode.appliedStyle(), null));
        }
        return result;
    }

    private ChapterNode getNodeAt(List<ChapterNode> chapters, List<Integer> path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        List<ChapterNode> current = chapters;
        ChapterNode node = null;
        for (int i = 0; i < path.size(); i++) {
            int idx = path.get(i);
            if (idx < 0 || idx >= current.size()) {
                return null;
            }
            node = current.get(idx);
            if (i == path.size() - 1) {
                return node;
            }
            if (node instanceof StoryNode story) {
                current = story.children();
            } else if (node instanceof ParagraphStyleRangeNode para) {
                current = para.children();
            } else {
                return null;
            }
        }
        return node;
    }

    private ChapterNode cloneWithChildren(ChapterNode parent, List<ChapterNode> newChildren) {
        List<ChapterNode> children = newChildren != null ? List.copyOf(newChildren) : Collections.emptyList();
        if (parent instanceof StoryNode story) {
            return new StoryNode(children, story.appliedStyle());
        }
        if (parent instanceof ParagraphStyleRangeNode para) {
            return new ParagraphStyleRangeNode(children, para.appliedStyle());
        }
        return parent;
    }

    private List<ChapterNode> replaceNodeAt(List<ChapterNode> nodes, List<Integer> path, int depth, ChapterNode newNode) {
        if (depth == path.size() - 1) {
            int idx = path.get(depth);
            List<ChapterNode> result = new ArrayList<>(nodes);
            result.set(idx, newNode);
            return result;
        }
        int idx = path.get(depth);
        ChapterNode child = nodes.get(idx);
        List<ChapterNode> newChildChildren = replaceNodeAt(child.children(), path, depth + 1, newNode);
        ChapterNode newChild = cloneWithChildren(child, newChildChildren);
        List<ChapterNode> result = new ArrayList<>(nodes);
        result.set(idx, newChild);
        return result;
    }
}
