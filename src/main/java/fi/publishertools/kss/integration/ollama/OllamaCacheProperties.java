package fi.publishertools.kss.integration.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for development-time caching of Ollama image description results.
 */
@ConfigurationProperties(prefix = "kss.ollama")
public class OllamaCacheProperties {

    /**
     * When true, image descriptions are read from and written to a file-based cache
     * so repeated runs reuse results without calling the model.
     */
    private boolean cacheEnabled = false;

    /**
     * Path to the cache file (e.g. JSON). Tilde (~) is resolved to user home.
     * Used only when {@link #cacheEnabled} is true.
     */
    private String cachePath = "~/.kss-ollama-cache/ollama-image-cache.json";

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }
}
