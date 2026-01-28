package fi.publishertools.kss.model;

/**
 * Internal value object for a file ready to be streamed in a download response.
 */
public class DownloadableFile {

    private final byte[] content;
    private final String fileName;
    private final String contentType;
    private final long contentLength;

    public DownloadableFile(byte[] content, String fileName, String contentType) {
        this.content = content;
        this.fileName = fileName;
        this.contentType = contentType;
        this.contentLength = content != null ? content.length : 0L;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }
}
