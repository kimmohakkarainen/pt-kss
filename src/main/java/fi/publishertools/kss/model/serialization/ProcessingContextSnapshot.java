package fi.publishertools.kss.model.serialization;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import fi.publishertools.kss.model.content.ChapterNode;
import fi.publishertools.kss.model.content.ImageNode;

/**
 * Serializable DTO for ProcessingContext.
 * Used to persist context to disk at end of A3 for development and debugging.
 * Uses Java object binary serialization.
 */
public record ProcessingContextSnapshot(
        String fileId,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant uploadTime,
        byte[] originalFileContents,
        byte[] packageOpf,
        List<byte[]> storiesList,
        List<ChapterNode> chapters,
        List<ImageNode> imageList,
        Map<String, byte[]> imageContent,
        byte[] xhtmlContent,
        byte[] tocContent,
        Map<String, String> metadata) implements Serializable {
}
