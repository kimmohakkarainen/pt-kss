package fi.publishertools.kss.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utilities for reading ZIP archives.
 */
public final class ZipUtils {

    private ZipUtils() {
    }

    /**
     * Extracts a single entry from a ZIP archive by name.
     * Entry names are normalized to use forward slashes for comparison.
     *
     * @param zipBytes  the ZIP archive bytes
     * @param entryName the entry name (backslashes are normalized to forward slashes)
     * @return the entry content, or null if not found or entryName is null/empty
     */
    public static byte[] extractEntry(byte[] zipBytes, String entryName) throws IOException {
        if (entryName == null || entryName.isEmpty()) {
            return null;
        }
        String normalized = entryName.replace('\\', '/');
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.equals(normalized)) {
                    return zis.readAllBytes();
                }
            }
        }
        return null;
    }

    /**
     * Decodes percent-encoded characters in a URI (e.g. %20 -> space, %C3%A4 -> ä)
     * and normalizes to NFC form.
     */
    public static String decodeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        try {
            String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            return Normalizer.normalize(decoded, Normalizer.Form.NFC);
        } catch (IllegalArgumentException e) {
            return uri;
        }
    }

    /**
     * Extracts the last path segment (filename) from a URI.
     */
    public static String extractFileNameFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return uri;
        }
        int lastSlash = uri.lastIndexOf('/');
        return lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
    }
}
