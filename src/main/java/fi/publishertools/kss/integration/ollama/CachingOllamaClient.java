package fi.publishertools.kss.integration.ollama;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Decorator that caches Ollama image description results on disk keyed by SHA-256 of image content.
 * Intended for development to avoid repeated slow model calls for the same images.
 */
public class CachingOllamaClient extends OllamaClient {

    private static final Logger logger = LoggerFactory.getLogger(CachingOllamaClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final OllamaClient delegate;
    private final Map<String, String> cache;
    private final Path cacheFile;

    public CachingOllamaClient(OllamaClient delegate, Path cacheFile) {
        this.delegate = delegate != null ? delegate : new OllamaClient();
        this.cacheFile = cacheFile;
        this.cache = loadCache();
    }

    @Override
    public Optional<String> describeImage(byte[] imageContent) {
        if (imageContent == null || imageContent.length == 0) {
            return Optional.empty();
        }
        String hash = sha256Hex(imageContent);
        String cached = cache.get(hash);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<String> result = delegate.describeImage(imageContent);
        if (result.isPresent()) {
            cache.put(hash, result.get());
            persistCache();
        }
        return result;
    }

    private Map<String, String> loadCache() {
        if (cacheFile == null || !Files.isRegularFile(cacheFile)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> loaded = OBJECT_MAPPER.readValue(cacheFile.toFile(), MAP_TYPE);
            return loaded != null ? new LinkedHashMap<>(loaded) : new LinkedHashMap<>();
        } catch (Exception e) {
            logger.warn("Could not load Ollama image cache from {}: {}", cacheFile, e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private void persistCache() {
        if (cacheFile == null) {
            return;
        }
        try {
            Path parent = cacheFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(cacheFile.toFile(), cache);
        } catch (Exception e) {
            logger.warn("Could not write Ollama image cache to {}: {}", cacheFile, e.getMessage());
        }
    }

    private static String sha256Hex(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
