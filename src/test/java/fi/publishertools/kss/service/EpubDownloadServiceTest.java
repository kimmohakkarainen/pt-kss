package fi.publishertools.kss.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.publishertools.kss.exception.EpubNotFoundException;
import fi.publishertools.kss.exception.ProcessingNotCompletedException;
import fi.publishertools.kss.model.DownloadableFile;
import fi.publishertools.kss.model.ProcessedResult;
import fi.publishertools.kss.processing.ProcessingStatus;

@ExtendWith(MockitoExtension.class)
class EpubDownloadServiceTest {

    private static final String FILE_ID = "test-file-id";
    private static final byte[] EPUB_BYTES = new byte[]{0x50, 0x4B, 0x03, 0x04};
    private static final String ORIGINAL_FILENAME = "my-book.zip";

    @Mock
    private ProcessedResultStore resultStore;

    @Mock
    private ProcessingStatusStore statusStore;

    private EpubDownloadService service;

    @BeforeEach
    void setUp() {
        service = new EpubDownloadService(resultStore, statusStore);
    }

    @Test
    @DisplayName("getEpub returns EPUB when result is READY and payload contains epubFile")
    void getEpub_ready_returnsDownloadableFile() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("epubFile", EPUB_BYTES);
        payload.put("originalFilename", ORIGINAL_FILENAME);
        ProcessedResult result = new ProcessedResult(FILE_ID, ProcessingStatus.READY, Instant.now(), payload);

        when(resultStore.getResult(FILE_ID)).thenReturn(Optional.of(result));

        DownloadableFile file = service.getEpub(FILE_ID);

        assertThat(file).isNotNull();
        assertThat(file.getContent()).isEqualTo(EPUB_BYTES);
        assertThat(file.getFileName()).isEqualTo("my-book.epub");
        assertThat(file.getContentType()).isEqualTo("application/epub+zip");
        assertThat(file.getContentLength()).isEqualTo(EPUB_BYTES.length);
    }

    @Test
    @DisplayName("getEpub throws EpubNotFoundException when result is ERROR")
    void getEpub_error_throwsEpubNotFound() {
        ProcessedResult result = new ProcessedResult(FILE_ID, ProcessingStatus.ERROR, Instant.now(), null, "Failed");
        when(resultStore.getResult(FILE_ID)).thenReturn(Optional.of(result));

        assertThatThrownBy(() -> service.getEpub(FILE_ID))
                .isInstanceOf(EpubNotFoundException.class)
                .hasMessageContaining("Processing failed");
    }

    @Test
    @DisplayName("getEpub throws ProcessingNotCompletedException when status is IN_PROGRESS")
    void getEpub_inProgress_throwsProcessingNotCompleted() {
        when(resultStore.getResult(FILE_ID)).thenReturn(Optional.empty());
        when(statusStore.getStatus(FILE_ID)).thenReturn(Optional.of(ProcessingStatus.IN_PROGRESS));

        assertThatThrownBy(() -> service.getEpub(FILE_ID))
                .isInstanceOf(ProcessingNotCompletedException.class)
                .hasMessageContaining("still in progress");
    }

    @Test
    @DisplayName("getEpub throws EpubNotFoundException when ID unknown")
    void getEpub_unknownId_throwsEpubNotFound() {
        when(resultStore.getResult(FILE_ID)).thenReturn(Optional.empty());
        when(statusStore.getStatus(FILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEpub(FILE_ID))
                .isInstanceOf(EpubNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getEpub derives filename from originalFilename without extension")
    void getEpub_derivesFileNameWithoutExtension() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("epubFile", EPUB_BYTES);
        payload.put("originalFilename", "noextension");
        ProcessedResult result = new ProcessedResult(FILE_ID, ProcessingStatus.READY, Instant.now(), payload);
        when(resultStore.getResult(FILE_ID)).thenReturn(Optional.of(result));

        DownloadableFile file = service.getEpub(FILE_ID);

        assertThat(file.getFileName()).isEqualTo("noextension.epub");
    }
}
