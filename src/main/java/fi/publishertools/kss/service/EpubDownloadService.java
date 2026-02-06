package fi.publishertools.kss.service;

import org.springframework.stereotype.Service;

import fi.publishertools.kss.exception.EpubNotFoundException;
import fi.publishertools.kss.exception.ProcessingNotCompletedException;
import fi.publishertools.kss.model.DownloadableFile;
import fi.publishertools.kss.model.ProcessedResult;
import fi.publishertools.kss.processing.ProcessingStatus;

/**
 * Service for retrieving ready-made EPUB files by upload/processing ID.
 */
@Service
public class EpubDownloadService {

    private static final String EPUB_CONTENT_TYPE = "application/epub+zip";
    private static final String DEFAULT_EPUB_FILENAME = "output.epub";
    private static final String PAYLOAD_EPUB_FILE = "epubFile";
    private static final String PAYLOAD_ORIGINAL_FILENAME = "originalFilename";

    private final ProcessedResultStore resultStore;
    private final ProcessingStatusStore statusStore;

    public EpubDownloadService(ProcessedResultStore resultStore, ProcessingStatusStore statusStore) {
        this.resultStore = resultStore;
        this.statusStore = statusStore;
    }

    /**
     * Returns the EPUB file for the given upload/processing ID if processing is complete.
     *
     * @param fileId upload/processing ID
     * @return downloadable EPUB file
     * @throws EpubNotFoundException if the ID is unknown or no EPUB is available
     * @throws ProcessingNotCompletedException if processing is still in progress (caller should return 202)
     */
    public DownloadableFile getEpub(String fileId) {
        ProcessedResult result = resultStore.getResult(fileId).orElse(null);

        if (result != null) {
            if (result.getStatus() == ProcessingStatus.READY) {
                byte[] epubBytes = getEpubBytesFromPayload(result);
                if (epubBytes == null) {
                    throw new EpubNotFoundException("EPUB not available for file id: " + fileId);
                }
                String fileName = deriveEpubFileName(result);
                return new DownloadableFile(epubBytes, fileName, EPUB_CONTENT_TYPE);
            }
            if (result.getStatus() == ProcessingStatus.ERROR) {
                throw new EpubNotFoundException("Processing failed for file id: " + fileId);
            }
        }

        if (statusStore.getStatus(fileId).orElse(null) == ProcessingStatus.IN_PROGRESS) {
            throw new ProcessingNotCompletedException("Processing still in progress for file id: " + fileId);
        }

        throw new EpubNotFoundException("EPUB not found for file id: " + fileId);
    }

    private byte[] getEpubBytesFromPayload(ProcessedResult result) {
        if (result.getPayload() == null) {
            return null;
        }
        Object epub = result.getPayload().get(PAYLOAD_EPUB_FILE);
        return epub instanceof byte[] ? (byte[]) epub : null;
    }

    private String deriveEpubFileName(ProcessedResult result) {
        if (result.getPayload() == null) {
            return DEFAULT_EPUB_FILENAME;
        }
        Object name = result.getPayload().get(PAYLOAD_ORIGINAL_FILENAME);
        if (name instanceof String && !((String) name).isEmpty()) {
            String base = (String) name;
            int lastDot = base.lastIndexOf('.');
            if (lastDot > 0) {
                return base.substring(0, lastDot) + ".epub";
            }
            return base + ".epub";
        }
        return DEFAULT_EPUB_FILENAME;
    }
}
