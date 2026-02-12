package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.integration.ollama.OllamaClient;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.CharacterStyleRangeNode;
import fi.publishertools.kss.model.content.ParagraphStyleRangeNode;
import fi.publishertools.kss.exception.AwaitingLangMarkupReviewException;
import fi.publishertools.kss.model.content.StoryNode;
import fi.publishertools.kss.phases.B3_ProposeLangMarkup;

class B3_ProposeLangMarkupTest {

	private StubOllamaLangClient stubClient;
	private B3_ProposeLangMarkup phase;

	@BeforeEach
	void setUp() {
		stubClient = new StubOllamaLangClient();
		phase = new B3_ProposeLangMarkup(stubClient);
	}

	@Test
	@DisplayName("mixed-language text node is split into multiple nodes by language segment and pauses for review")
	void splitsMixedLanguageNode() throws Exception {
		String text = "Tämä on hello world lause.";
		CharacterStyleRangeNode textNode = new CharacterStyleRangeNode(text, "CharacterStyle/Default", null);
		ParagraphStyleRangeNode paragraph = new ParagraphStyleRangeNode(List.of(textNode), "ParagraphStyle/Body");
		StoryNode story = new StoryNode(List.of(paragraph), "TOCStyle/Chapter");

		ProcessingContext context = contextWithChapters(List.of(story));
		stubClient.setResponse("{\"words\": [\"hello\", \"world\"]}");

		assertThatThrownBy(() -> phase.process(context))
				.isInstanceOf(AwaitingLangMarkupReviewException.class)
				.extracting(ex -> ((AwaitingLangMarkupReviewException) ex).getContext())
				.satisfies(ctx -> {
					ChapterNode outStory = ctx.getChapters().get(0);
					ChapterNode outPara = outStory.children().get(0);
					List<ChapterNode> children = outPara.children();
					assertThat(children).hasSize(3);
					assertThat(((CharacterStyleRangeNode) children.get(0)).text()).isEqualTo("Tämä on ");
					assertThat(((CharacterStyleRangeNode) children.get(1)).text()).isEqualTo("hello world");
					assertThat(((CharacterStyleRangeNode) children.get(1)).language()).isEqualTo("und");
					assertThat(((CharacterStyleRangeNode) children.get(2)).text()).isEqualTo(" lause.");
					assertThat(((CharacterStyleRangeNode) children.get(0)).appliedStyle()).isEqualTo("CharacterStyle/Default");
					assertThat(((CharacterStyleRangeNode) children.get(1)).appliedStyle()).isEqualTo("CharacterStyle/Default");
				});
	}

	@Test
	@DisplayName("empty Ollama response leaves node unsplit")
	void emptyResponseLeavesUnsplit() throws Exception {
		CharacterStyleRangeNode textNode = new CharacterStyleRangeNode("Tämä on teksti.", "CharacterStyle/Default", null);
		ParagraphStyleRangeNode paragraph = new ParagraphStyleRangeNode(List.of(textNode), "ParagraphStyle/Body");
		ProcessingContext context = contextWithChapters(List.of(new StoryNode(List.of(paragraph), null)));
		stubClient.setResponse(null);

		phase.process(context);

		ChapterNode outPara = context.getChapters().get(0).children().get(0);
		assertThat(outPara.children()).hasSize(1);
		assertThat(((CharacterStyleRangeNode) outPara.children().get(0)).text()).isEqualTo("Tämä on teksti.");
	}

	@Test
	@DisplayName("invalid or empty words array leaves node unsplit")
	void invalidJsonLeavesUnsplit() throws Exception {
		CharacterStyleRangeNode textNode = new CharacterStyleRangeNode("Some text", "CharacterStyle/Default", null);
		ParagraphStyleRangeNode paragraph = new ParagraphStyleRangeNode(List.of(textNode), "ParagraphStyle/Body");
		ProcessingContext context = contextWithChapters(List.of(new StoryNode(List.of(paragraph), null)));
		stubClient.setResponse("{\"words\": []}");

		phase.process(context);

		ChapterNode outPara = context.getChapters().get(0).children().get(0);
		assertThat(outPara.children()).hasSize(1);
		assertThat(((CharacterStyleRangeNode) outPara.children().get(0)).text()).isEqualTo("Some text");
	}

	@Test
	@DisplayName("blank text node is not sent to Ollama and left unchanged")
	void blankTextNodeUnchanged() throws Exception {
		CharacterStyleRangeNode textNode = new CharacterStyleRangeNode("", "CharacterStyle/Default", null);
		ParagraphStyleRangeNode paragraph = new ParagraphStyleRangeNode(List.of(textNode), "ParagraphStyle/Body");
		ProcessingContext context = contextWithChapters(List.of(new StoryNode(List.of(paragraph), null)));

		phase.process(context);

		assertThat(stubClient.invocationCount()).isZero();
		ChapterNode outPara = context.getChapters().get(0).children().get(0);
		assertThat(outPara.children()).hasSize(1);
		assertThat(((CharacterStyleRangeNode) outPara.children().get(0)).text()).isEmpty();
	}

	@Test
	@DisplayName("null or empty chapters does nothing")
	void emptyChaptersNoOp() throws Exception {
		ProcessingContext context = contextWithChapters(List.of());
		phase.process(context);
		assertThat(context.getChapters()).isEmpty();
	}

	@Test
	@DisplayName("Ollama failure leaves node unsplit")
	void ollamaFailureLeavesUnsplit() throws Exception {
		CharacterStyleRangeNode textNode = new CharacterStyleRangeNode("Hello world", "CharacterStyle/Default", null);
		ParagraphStyleRangeNode paragraph = new ParagraphStyleRangeNode(List.of(textNode), "ParagraphStyle/Body");
		ProcessingContext context = contextWithChapters(List.of(new StoryNode(List.of(paragraph), null)));
		stubClient.setThrowOnInvocation(true);

		phase.process(context);

		ChapterNode outPara = context.getChapters().get(0).children().get(0);
		assertThat(outPara.children()).hasSize(1);
		assertThat(((CharacterStyleRangeNode) outPara.children().get(0)).text()).isEqualTo("Hello world");
	}

	@Test
	@DisplayName("JSON with code fence is parsed correctly and pauses for review")
	void jsonWithCodeFenceParsed() throws Exception {
		String text = "Start foo end.";
		CharacterStyleRangeNode textNode = new CharacterStyleRangeNode(text, null, null);
		ParagraphStyleRangeNode paragraph = new ParagraphStyleRangeNode(List.of(textNode), null);
		ProcessingContext context = contextWithChapters(List.of(new StoryNode(List.of(paragraph), null)));
		stubClient.setResponse("```json\n{\"words\": [\"foo\"]}\n```");

		assertThatThrownBy(() -> phase.process(context))
				.isInstanceOf(AwaitingLangMarkupReviewException.class)
				.extracting(ex -> ((AwaitingLangMarkupReviewException) ex).getContext())
				.satisfies(ctx -> {
					ChapterNode outPara = ctx.getChapters().get(0).children().get(0);
					assertThat(outPara.children()).hasSize(3);
					assertThat(((CharacterStyleRangeNode) outPara.children().get(0)).text()).isEqualTo("Start ");
					assertThat(((CharacterStyleRangeNode) outPara.children().get(1)).text()).isEqualTo("foo");
					assertThat(((CharacterStyleRangeNode) outPara.children().get(2)).text()).isEqualTo(" end.");
				});
	}

	private static ProcessingContext contextWithChapters(List<ChapterNode> chapters) {
		ProcessingContext context = new ProcessingContext(
				new StoredFile("f1", "x.idml", "application/zip", 0L, java.time.Instant.EPOCH, new byte[0]));
		context.setChapters(chapters);
		return context;
	}

	private static final class StubOllamaLangClient extends OllamaClient {
		private Optional<String> response = Optional.empty();
		private boolean throwOnInvocation;
		private int invocationCount;

		void setResponse(String json) {
			this.response = json != null ? Optional.of(json) : Optional.empty();
		}

		void setThrowOnInvocation(boolean b) {
			this.throwOnInvocation = b;
		}

		int invocationCount() {
			return invocationCount;
		}

		@Override
		public Optional<String> detectNonMainLanguageWords(String text, String mainLanguage) {
			invocationCount++;
			if (throwOnInvocation) {
				throw new RuntimeException("simulated Ollama failure");
			}
			return response;
		}
	}
}
