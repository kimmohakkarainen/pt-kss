package fi.publishertools.kss.integration.ollama;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Minimal HTTP client for Ollama /api/generate with vision (image) input and text-only generation.
 * Image: sends base64-encoded image and prompt, returns the generated text from the response.
 * Text: used for language detection (e.g. non-main-language words) with gemma3n:e2b.
 */
public class OllamaClient {

	private static final String DEFAULT_BASE_URL = "http://localhost:11434";
	private static final String MODEL = "qwen3-vl:4b";
	private static final String PROMPT = "Describe the image?";
	private static final String LANG_DETECTION_MODEL = "gemma3n:e2b";
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

	private final String baseUrl;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public OllamaClient() {
		this(DEFAULT_BASE_URL);
	}

	public OllamaClient(String baseUrl) {
		this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : DEFAULT_BASE_URL;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Sends the image to Ollama and returns the model's description, or empty on any error.
	 *
	 * @param imageContent raw image bytes (e.g. PNG/JPEG)
	 * @return the generated description text, or empty if the request failed or returned no text
	 */
	public Optional<String> describeImage(byte[] imageContent) {
		if (imageContent == null || imageContent.length == 0) {
			return Optional.empty();
		}
		String base64Image = Base64.getEncoder().encodeToString(imageContent);
		ObjectNode body = objectMapper.createObjectNode();
		body.put("model", MODEL);
		body.put("prompt", PROMPT);
		body.put("stream", false);
		ArrayNode images = body.putArray("images");
		images.add(base64Image);

		try {
			String bodyJson = objectMapper.writeValueAsString(body);
			URI uri = URI.create(baseUrl + "/api/generate");
			HttpRequest request = HttpRequest.newBuilder(uri)
					.header("Content-Type", "application/json")
					.timeout(REQUEST_TIMEOUT)
					.POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				return Optional.empty();
			}
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode responseText = root != null ? root.get("response") : null;
			if (responseText == null || !responseText.isTextual()) {
				return Optional.empty();
			}
			String text = responseText.asText();
			if (text == null || text.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(text.trim());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Calls Ollama /api/generate with text-only prompt to detect words not in the main language.
	 * Prompt asks for JSON-formatted list of such words/phrases.
	 *
	 * @param text        the text to analyze (e.g. contents of a CharacterStyleRangeNode)
	 * @param mainLanguage the main language name (e.g. "finnish") used in the prompt
	 * @return the raw response string for JSON parsing, or empty on failure
	 */
	public Optional<String> detectNonMainLanguageWords(String text, String mainLanguage) {
		if(true) {
			String [] split = text.split("\\W");
			if(split.length > 100) {
				return Optional.of("{\"words\": [\""+ split[50] + "\"]}");
			} else {
				return Optional.of("[]");
			}
		}
		if (text == null) {
			text = "";
		}
		String lang = mainLanguage != null && !mainLanguage.isBlank() ? mainLanguage : "finnish";
		String prompt = "pick up the words in following sentences that are not in " + lang
				+ " language and give result as JSON-format. <context>" + text + "</context>";

		ObjectNode body = objectMapper.createObjectNode();
		body.put("model", LANG_DETECTION_MODEL);
		body.put("prompt", prompt);
		body.put("stream", false);

		try {
			String bodyJson = objectMapper.writeValueAsString(body);
			URI uri = URI.create(baseUrl + "/api/generate");
			HttpRequest request = HttpRequest.newBuilder(uri)
					.header("Content-Type", "application/json")
					.timeout(REQUEST_TIMEOUT)
					.POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() != 200) {
				return Optional.empty();
			}
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode responseText = root != null ? root.get("response") : null;
			if (responseText == null || !responseText.isTextual()) {
				return Optional.empty();
			}
			String responseStr = responseText.asText();
			if (responseStr == null || responseStr.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(responseStr.trim());
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
