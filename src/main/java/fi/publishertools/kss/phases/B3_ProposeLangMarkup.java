package fi.publishertools.kss.phases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.publishertools.kss.integration.ollama.OllamaClient;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Detects words/phrases not in the main language (default Finnish) in each CharacterStyleRangeNode
 * via Ollama text generation, and splits such nodes into multiple CharacterStyleRangeNodes so each
 * contains a single language segment.
 */
public class B3_ProposeLangMarkup extends ProcessingPhase {

	private static final Logger logger = LoggerFactory.getLogger(B3_ProposeLangMarkup.class);
	private static final String DEFAULT_MAIN_LANGUAGE = "finnish";

	/** JSON key we ask the model to use for the list of non-main-language words/phrases. */
	private static final String JSON_KEY_WORDS = "words";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	/** Matches optional markdown code fence around JSON (e.g. ```json ... ```). */
	private static final Pattern CODE_FENCE = Pattern.compile("^\\s*```(?:json)?\\s*\\n?(.*)\\n?```\\s*$", Pattern.DOTALL);

	private final OllamaClient ollamaClient;

	public B3_ProposeLangMarkup() {
		this(new OllamaClient());
	}

	public B3_ProposeLangMarkup(OllamaClient ollamaClient) {
		this.ollamaClient = ollamaClient != null ? ollamaClient : new OllamaClient();
	}

	@Override
	public void process(ProcessingContext context) throws Exception {
		List<ChapterNode> chapters = context.getChapters();
		if (chapters == null || chapters.isEmpty()) {
			return;
		}
		String mainLanguage = mainLanguageFromContext(context);
		List<ChapterNode> updated = processNodes(chapters, mainLanguage, context.getFileId());
		context.setChapters(updated);
	}

	private String mainLanguageFromContext(ProcessingContext context) {
		String lang = context.getMetadata("language", String.class);
		return lang != null && !lang.isBlank() ? lang : DEFAULT_MAIN_LANGUAGE;
	}

	private List<ChapterNode> processNodes(List<ChapterNode> input, String mainLanguage, String fileId) {
		List<ChapterNode> output = new ArrayList<>();
		for (ChapterNode node : input) {
			if (node instanceof StoryNode story) {
				List<ChapterNode> children = processNodes(story.children(), mainLanguage, fileId);
				output.add(new StoryNode(children, story.appliedStyle()));
			} else if (node instanceof ParagraphStyleRangeNode para) {
				List<ChapterNode> children = processNodes(para.children(), mainLanguage, fileId);
				output.add(new ParagraphStyleRangeNode(children, para.appliedStyle()));
			} else if (node instanceof CharacterStyleRangeNode textNode) {
				List<ChapterNode> replacement = processTextNode(textNode, mainLanguage, fileId);
				output.addAll(replacement);
			} else if (node instanceof ImageNode) {
				output.add(node);
			}
		}
		return output;
	}

	/**
	 * Either returns a singleton list with the same node (no split) or a list of new
	 * CharacterStyleRangeNodes for each language segment.
	 */
	private List<ChapterNode> processTextNode(CharacterStyleRangeNode node, String mainLanguage, String fileId) {
		String text = node.text();
		if (text == null || text.isBlank()) {
			return List.of(node);
		}

		Optional<String> responseOpt;
		try {
			responseOpt = ollamaClient.detectNonMainLanguageWords(text, mainLanguage);
		} catch (Exception e) {
			logger.warn("Ollama lang detection failed for file {} text node: {}, leaving unsplit", fileId, e.getMessage());
			return List.of(node);
		}
		if (responseOpt.isEmpty()) {
			logger.warn("Ollama lang detection failed for file {} text node, leaving unsplit", fileId);
			return List.of(node);
		}

		List<String> words = parseWordsFromJson(responseOpt.get());
		if (words == null || words.isEmpty()) {
			return List.of(node);
		}

		List<Segment> segments = splitIntoSegments(text, words);
		if (segments.size() <= 1) {
			return List.of(node);
		}

		String appliedStyle = node.appliedStyle();
		List<ChapterNode> result = new ArrayList<>();
		for (Segment seg : segments) {
			if (seg.text().isEmpty()) {
				continue;
			}
			result.add(new CharacterStyleRangeNode(seg.text(), appliedStyle));
		}
		return result.isEmpty() ? List.of(node) : result;
	}

	/**
	 * Parse list of strings from Ollama response. Expects JSON like {"words": ["a", "b"]}.
	 * Strips optional markdown code fences.
	 */
	List<String> parseWordsFromJson(String raw) {
		String json = raw.trim();
		// Strip ```json ... ``` if present
		var matcher = CODE_FENCE.matcher(json);
		if (matcher.matches()) {
			json = matcher.group(1).trim();
		}
		try {
			JsonNode root = OBJECT_MAPPER.readTree(json);
			if (root == null || !root.isObject()) {
				return Collections.emptyList();
			}
			JsonNode wordsNode = root.get(JSON_KEY_WORDS);
			if (wordsNode == null) {
				// Try alternative key
				wordsNode = root.get("nonMainLanguage");
			}
			if (wordsNode == null || !wordsNode.isArray()) {
				return Collections.emptyList();
			}
			List<String> list = new ArrayList<>();
			for (JsonNode item : wordsNode) {
				if (item != null && item.isTextual()) {
					String s = item.asText();
					if (s != null && !s.isBlank()) {
						list.add(s.trim());
					}
				}
			}
			return list;
		} catch (Exception e) {
			logger.debug("Failed to parse Ollama JSON response: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Split text into alternating main/other segments using the list of non-main-language
	 * phrases. Finds all occurrences, merges overlapping ranges, then builds segments.
	 */
	List<Segment> splitIntoSegments(String text, List<String> nonMainPhrases) {
		if (text == null || text.isEmpty() || nonMainPhrases == null || nonMainPhrases.isEmpty()) {
			return List.of(new Segment(text != null ? text : "", true));
		}

		List<int[]> ranges = new ArrayList<>();
		for (String phrase : nonMainPhrases) {
			if (phrase == null || phrase.isEmpty()) {
				continue;
			}
			int start = 0;
			while (true) {
				int idx = text.indexOf(phrase, start);
				if (idx < 0) {
					break;
				}
				ranges.add(new int[] { idx, idx + phrase.length() });
				start = idx + 1;
			}
		}
		if (ranges.isEmpty()) {
			return List.of(new Segment(text, true));
		}

		// Sort by start, then merge overlapping or adjacent ranges
		ranges.sort((a, b) -> Integer.compare(a[0], b[0]));
		List<int[]> merged = new ArrayList<>();
		int[] cur = ranges.get(0);
		for (int i = 1; i < ranges.size(); i++) {
			int[] next = ranges.get(i);
			if (next[0] <= cur[1] + 1) {
				cur[1] = Math.max(cur[1], next[1]);
			} else {
				merged.add(cur);
				cur = next;
			}
		}
		merged.add(cur);

		// Build segments: [main][other][main][other]...
		List<Segment> segments = new ArrayList<>();
		int pos = 0;
		for (int[] r : merged) {
			if (r[0] > pos) {
				segments.add(new Segment(text.substring(pos, r[0]), true));
			}
			segments.add(new Segment(text.substring(r[0], r[1]), false));
			pos = r[1];
		}
		if (pos < text.length()) {
			segments.add(new Segment(text.substring(pos), true));
		}
		return segments;
	}

	private record Segment(String text, boolean mainLanguage) {}
}
