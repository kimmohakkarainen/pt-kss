package fi.publishertools.kss.service;

import org.springframework.stereotype.Component;

import fi.publishertools.kss.model.StoredFile;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFileStore {

    private final Map<String, StoredFile> store = new ConcurrentHashMap<>();

    public void save(StoredFile file) {
        store.put(file.getId(), file);
    }

    public Optional<StoredFile> find(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public void remove(String id) {
        store.remove(id);
    }
}

