package fi.publishertools.kss.integration.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CachingOllamaClientTest {

    @TempDir
    Path tempDir;

    private Path cacheFile;
    private CountingStubOllamaClient stub;

    @BeforeEach
    void setUp() {
        cacheFile = tempDir.resolve("ollama-image-cache.json");
        stub = new CountingStubOllamaClient();
    }

    @Test
    @DisplayName("cache miss delegates to Ollama and stores result; cache hit returns stored value without delegating")
    void cacheHitSkipsDelegate() {
        byte[] imageBytes = new byte[] { 1, 2, 3 };
        stub.setResult(Optional.of("Cached description"));

        CachingOllamaClient client = new CachingOllamaClient(stub, cacheFile);

        Optional<String> first = client.describeImage(imageBytes);
        assertThat(first).hasValue("Cached description");
        assertThat(stub.invocationCount()).isEqualTo(1);

        Optional<String> second = client.describeImage(imageBytes);
        assertThat(second).hasValue("Cached description");
        assertThat(stub.invocationCount()).isEqualTo(1);

        assertThat(cacheFile).exists();
    }

    @Test
    @DisplayName("empty delegate result is not cached")
    void emptyResultNotCached() {
        byte[] imageBytes = new byte[] { 4, 5, 6 };
        stub.setResult(Optional.empty());

        CachingOllamaClient client = new CachingOllamaClient(stub, cacheFile);

        assertThat(client.describeImage(imageBytes)).isEmpty();
        assertThat(client.describeImage(imageBytes)).isEmpty();
        assertThat(stub.invocationCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("cache file is reused across CachingOllamaClient instances")
    void cachePersistedAcrossInstances() throws Exception {
        byte[] imageBytes = new byte[] { 7, 8, 9 };
        stub.setResult(Optional.of("Persisted description"));

        CachingOllamaClient client1 = new CachingOllamaClient(stub, cacheFile);
        assertThat(client1.describeImage(imageBytes)).hasValue("Persisted description");
        assertThat(stub.invocationCount()).isEqualTo(1);

        CountingStubOllamaClient stub2 = new CountingStubOllamaClient();
        stub2.setResult(Optional.of("Should not be used"));
        CachingOllamaClient client2 = new CachingOllamaClient(stub2, cacheFile);
        Optional<String> fromSecond = client2.describeImage(imageBytes);

        assertThat(fromSecond).hasValue("Persisted description");
        assertThat(stub2.invocationCount()).isZero();
    }

    @Test
    @DisplayName("different image content produces different cache entries")
    void differentImagesDifferentEntries() {
        stub.setResult(Optional.of("Desc A"));
        CachingOllamaClient client = new CachingOllamaClient(stub, cacheFile);

        byte[] imageA = new byte[] { 10, 20 };
        byte[] imageB = new byte[] { 10, 21 };

        client.describeImage(imageA);
        stub.setResult(Optional.of("Desc B"));
        client.describeImage(imageB);

        assertThat(stub.invocationCount()).isEqualTo(2);
        assertThat(client.describeImage(imageA)).hasValue("Desc A");
        assertThat(client.describeImage(imageB)).hasValue("Desc B");
        assertThat(stub.invocationCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("missing or unreadable cache file starts empty")
    void missingCacheFileStartsEmpty() {
        Path nonExistent = tempDir.resolve("nonexistent").resolve("cache.json");
        stub.setResult(Optional.of("Only from delegate"));

        CachingOllamaClient client = new CachingOllamaClient(stub, nonExistent);
        byte[] imageBytes = new byte[] { 1 };

        assertThat(client.describeImage(imageBytes)).hasValue("Only from delegate");
        assertThat(Files.exists(nonExistent)).isTrue();
    }

    private static final class CountingStubOllamaClient extends OllamaClient {
        private Optional<String> result = Optional.empty();
        private int invocationCount;

        void setResult(Optional<String> result) {
            this.result = result;
        }

        int invocationCount() {
            return invocationCount;
        }

        @Override
        public Optional<String> describeImage(byte[] imageContent) {
            invocationCount++;
            return result;
        }
    }
}
