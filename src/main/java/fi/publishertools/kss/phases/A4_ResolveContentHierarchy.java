package fi.publishertools.kss.phases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ImageNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
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

		StyleTable table = collectStyles(reorganized, new StyleTable());

		logger.info(table.toString());


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

	private StyleTable collectStyles(List<ChapterNode> input, StyleTable output) {

		for(ChapterNode node : input) {
			if(node.appliedStyle() != null) {
				output.add(node.appliedStyle());
			}
			if(node instanceof StoryNode) {
				output.push();
				output = collectStyles(node.children(), output);
				output.pop();
			} else if(node instanceof ParagraphStyleRangeNode) {
				output.push();
				output = collectStyles(node.children(), output);
				output.pop();
			}  
		}
		return output;
	}

	public static class StyleTable {

		private Map<String, Map<String, Long>> table = new TreeMap<>();
		private String previous = "";
		private Stack<String> stack = new Stack<>();
		
		
		public void add(String next) {
			put(this.previous, next);
			this.previous = next;
		}

		public void addAll(Collection<String> col) {
			for(String next : col) {
				this.add(next);
			}
		}
		
		public void push() {
			stack.push(this.previous);
			this.previous = "";
		}
		
		public void pop() {
			this.previous = stack.pop();
		}
		

		private void put(String previous, String next) {
			checkExistence(previous, next);
			Map<String, Long> row = table.get(previous);
			row.put(next,  1L + row.get(next));
		}

		private void checkExistence(String previous, String next) {
			checkExistence(previous);
			checkExistence(next);
		}

		private void checkExistence(String word) {
			Map<String, Long> row = table.get(word);
			if(row == null) {
				row = new TreeMap<>();
				table.put(word, row);
				for(String key : table.keySet()) {
					row.put(key, 0L);
					table.get(key).put(word, 0L);
				}
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("\n,");
			for(String col: table.keySet()) {
				builder.append(col);
				builder.append(",");
			}
			builder.append("\n");
			for(String row: table.keySet()) {
				builder.append(row);
				builder.append(",");
				for(String col: table.keySet()) {
					builder.append(table.get(row).get(col));
					builder.append(",");
				}
				builder.append("\n");
			}
			return builder.toString();
		}
	}
}
