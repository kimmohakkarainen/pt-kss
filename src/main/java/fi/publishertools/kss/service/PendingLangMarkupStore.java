package fi.publishertools.kss.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import fi.publishertools.kss.model.ProcessingContext;

/**
 * Thread-safe store for ProcessingContexts awaiting user review of proposed language markup.
 */
@Component
public class PendingLangMarkupStore {

    private final ConcurrentHashMap<String, ProcessingContext> store = new ConcurrentHashMap<>();

    public void store(String fileId, ProcessingContext context) {
        store.put(fileId, context);
    }

    public Optional<ProcessingContext> get(String fileId) {
        return Optional.ofNullable(store.get(fileId));
    }

    public Optional<ProcessingContext> remove(String fileId) {
        return Optional.ofNullable(store.remove(fileId));
    }

    public List<String> listFileIds() {
        return List.copyOf(store.keySet());
    }
}
