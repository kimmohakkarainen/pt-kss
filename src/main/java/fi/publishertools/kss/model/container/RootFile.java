package fi.publishertools.kss.model.container;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name="rootfile")
public class RootFile {
    
    public static RootFile create(String fullPath, String mediaType) {
	RootFile target = new RootFile();
	target.fullPath = fullPath;
	target.mediaType = mediaType;
	return target;
    }
    
    @XmlAttribute(name="full-path", required=true)    
    private String fullPath;

    @XmlAttribute(name="media-type", required=true)
    private String mediaType;

    public String getFullPath() {
        return fullPath;
    }

    public String getMediaType() {
        return mediaType;
    }
}
