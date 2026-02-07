package fi.publishertools.kss.phases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.model.content.SectionNode;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Phase that runs after A3_ExtractImageInfo. Reorganizes the chapter content hierarchy
 * based on paragraph style definitions from IDML design (Resources/Styles.xml).
 * Uses BasedOn chains to determine hierarchy levels and nests content accordingly.
 */
public class A4_ResolveContentHierarchy extends ProcessingPhase {

	private static final Logger logger = LoggerFactory.getLogger(A4_ResolveContentHierarchy.class);

	@Override
	public void process(ProcessingContext context) throws Exception, IOException {
		logger.debug("Resolving content hierarchy for file {}", context.getFileId());

		List<ChapterNode> chapters = context.getChapters();
		if (chapters == null || chapters.isEmpty()) {
			logger.debug("No chapters for file {}, nothing to resolve", context.getFileId());
			return;
		}

		List<ChapterNode> reorganized = simplifyStyles(chapters);

		context.setChapters(reorganized);
		logger.debug("Resolved content hierarchy for file {}", context.getFileId());
	}


	private List<ChapterNode> simplifyStyles(List<ChapterNode> input) {
		List<ChapterNode> output = new ArrayList<>();

		for(ChapterNode node : input) {
			if(node instanceof StoryNode) {
				List<ChapterNode> children = simplifyStyles(node.children());
				if(children != null) {
					output.add(new StoryNode(children, simplifyStyle(node.appliedStyle())));
				}
			} else if(node instanceof ParagraphStyleRangeNode) {
				List<ChapterNode> children = simplifyStyles(node.children());
				if(children != null) {
					output.add(new ParagraphStyleRangeNode(children, simplifyStyle(node.appliedStyle())));
				}
			} else if(node instanceof CharacterStyleRangeNode) {
				output.add(new CharacterStyleRangeNode(node.text(), simplifyStyle(node.appliedStyle())));
			} else if(node instanceof ImageNode) {
				output.add(new ImageNode(node.imageRef(), simplifyStyle(node.appliedStyle())));
			} else if(node instanceof SectionNode) {
				List<ChapterNode> children = simplifyStyles(node.children());
				if(children != null) {
					output.add(new SectionNode(node.title(), children));
				}
			} else {
				// do nothing
			}
		}
		return output.isEmpty() ? null : output;

	}

	private String simplifyStyle(String style) {
		String [] splitted = style == null ? null : style.split("/");
		return splitted == null ? null : splitted[splitted.length - 1];
	}
}
