package fi.publishertools.kss.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fi.publishertools.kss.model.DownloadableFile;
import fi.publishertools.kss.model.EpubNotFoundException;
import fi.publishertools.kss.model.ProcessingNotCompletedException;
import fi.publishertools.kss.service.EpubDownloadService;

@WebMvcTest(DownloadController.class)
class DownloadControllerTest {

    private static final String EPUB_MEDIA_TYPE = "application/epub+zip";
    private static final byte[] SAMPLE_EPUB = new byte[]{0x50, 0x4B, 0x03, 0x04}; // ZIP magic

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EpubDownloadService epubDownloadService;

    @Test
    @DisplayName("GET /api/v1/epub/{id} returns 200 with EPUB when ready")
    void downloadEpub_ready_returnsOkWithEpub() throws Exception {
        String fileId = "file-123";
        String fileName = "my-book.epub";
        DownloadableFile file = new DownloadableFile(SAMPLE_EPUB, fileName, EPUB_MEDIA_TYPE);

        when(epubDownloadService.getEpub(fileId)).thenReturn(file);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/epub/{id}", fileId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(EPUB_MEDIA_TYPE))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition",
                        "attachment; filename=\"" + fileName + "\""))
                .andExpect(MockMvcResultMatchers.content().bytes(SAMPLE_EPUB));

        verify(epubDownloadService).getEpub(eq(fileId));
    }

    @Test
    @DisplayName("GET /api/v1/epub/{id} returns 404 when EPUB not found")
    void downloadEpub_notFound_returns404() throws Exception {
        String fileId = "unknown-id";
        when(epubDownloadService.getEpub(fileId)).thenThrow(new EpubNotFoundException("EPUB not found for file id: " + fileId));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/epub/{id}", fileId))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("EPUB not found for file id: " + fileId));

        verify(epubDownloadService).getEpub(eq(fileId));
    }

    @Test
    @DisplayName("GET /api/v1/epub/{id} returns 202 when processing not yet complete")
    void downloadEpub_inProgress_returns202Accepted() throws Exception {
        String fileId = "file-456";
        when(epubDownloadService.getEpub(fileId))
                .thenThrow(new ProcessingNotCompletedException("Processing still in progress for file id: " + fileId));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/epub/{id}", fileId))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.ACCEPTED.value()))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("in-progress"));

        verify(epubDownloadService).getEpub(eq(fileId));
    }
}
