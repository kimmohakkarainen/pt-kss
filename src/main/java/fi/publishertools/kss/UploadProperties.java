package fi.publishertools.kss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kss.upload")
public class UploadProperties {

    /**
     * Maximum allowed upload size in bytes.
     */
    private long maxSizeBytes;

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }
}

