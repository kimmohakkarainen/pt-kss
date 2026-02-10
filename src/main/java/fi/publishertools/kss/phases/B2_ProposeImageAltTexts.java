package fi.publishertools.kss.phases;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.integration.ollama.OllamaClient;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Proposes alternate text for each image by sending image content to an Ollama
 * vision model (qwen3-vl:4b). Only fills alternateText when it is missing; does not overwrite.
 * On Ollama failure for an image, logs a warning and continues with the rest.
 */
public class B2_ProposeImageAltTexts extends ProcessingPhase {

	private static final Logger logger = LoggerFactory.getLogger(B2_ProposeImageAltTexts.class);

	private final OllamaClient ollamaClient;

	public B2_ProposeImageAltTexts() {
		this(new OllamaClient());
	}

	public B2_ProposeImageAltTexts(OllamaClient ollamaClient) {
		this.ollamaClient = ollamaClient != null ? ollamaClient : new OllamaClient();
	}

	@Override
	public void process(ProcessingContext context) {
		List<ImageNode> imageList = context.getImageList();
		if (imageList == null || imageList.isEmpty()) {
			return;
		}

		// First pass: call Ollama once per eligible filename and collect descriptions.
		Map<String, String> altByFileName = buildAltTextMap(context, imageList);
		if (altByFileName.isEmpty()) {
			return;
		}

		// Second pass: mutate ImageNode instances in the flat image list.
		for (ImageNode node : imageList) {
			String fileName = node.fileName();
			if (fileName == null || fileName.isBlank()) {
				continue;
			}
			if (node.alternateText() != null && !node.alternateText().isBlank()) {
				continue;
			}
			String alt = altByFileName.get(fileName);
			if (alt != null && !alt.isBlank()) {
				node.setAlternateText(alt);
			}
		}

		// Third pass: mutate ImageNode instances in the chapter content hierarchy.
		List<ChapterNode> chapters = context.getChapters();
		if (chapters != null && !chapters.isEmpty()) {
			for (ChapterNode node : chapters) {
				mutateChapterNode(node, altByFileName);
			}
		}
	}

	private Map<String, String> buildAltTextMap(ProcessingContext context, List<ImageNode> imageList) {
		Map<String, String> altByFileName = new LinkedHashMap<>();

		for (ImageNode node : imageList) {
			String fileName = node.fileName();
			if (fileName == null || fileName.isBlank()) {
				continue;
			}
			if (node.alternateText() != null && !node.alternateText().isBlank()) {
				continue;
			}
			if (altByFileName.containsKey(fileName)) {
				continue; // already generated for this filename
			}
			byte[] content = context.getImageContent(fileName);
			if (content == null || content.length == 0) {
				continue;
			}

			try {
				Optional<String> description = ollamaClient.describeImage(content);
				if (description.isEmpty()) {
					logger.warn("No alt text from Ollama for file {} image {}", context.getFileId(), fileName);
					continue;
				}
				altByFileName.put(fileName, description.get());
			} catch (Exception e) {
				logger.warn("Ollama failed for file {} image {}: {}", context.getFileId(), fileName, e.getMessage());
			}
		}

		return altByFileName;
	}

	private void mutateChapterNode(ChapterNode node, Map<String, String> altByFileName) {
		if (node instanceof StoryNode story) {
			for (ChapterNode child : story.children()) {
				mutateChapterNode(child, altByFileName);
			}
		} else if (node instanceof ParagraphStyleRangeNode para) {
			for (ChapterNode child : para.children()) {
				mutateChapterNode(child, altByFileName);
			}
		} else if (node instanceof ImageNode img) {
			String fileName = img.fileName();
			if (fileName == null || fileName.isBlank()) {
				return;
			}
			if (img.alternateText() != null && !img.alternateText().isBlank()) {
				return;
			}
			String alt = altByFileName.get(fileName);
			if (alt != null && !alt.isBlank()) {
				img.setAlternateText(alt);
			}
		}
	}
}
