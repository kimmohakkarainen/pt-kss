package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "item", namespace = "http://www.idpf.org/2007/opf")
public class ManifestItem {

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "href")
    private String href;

    @XmlAttribute(name = "media-type")
    private String mediaType;

    @XmlAttribute(name = "properties")
    private String properties;

    public String getId() {
        return id;
    }

    public String getHref() {
        return href;
    }
    
    public String getMediaType() {
        return mediaType;
    }

    public String getProperties() {
        return properties;
    }

    public static ManifestItem create(String id, String href, String mediaType, String properties) {
    	ManifestItem item = new ManifestItem();
    	item.id = id;
    	item.href = href;
    	item.mediaType = mediaType;
    	item.properties = properties;
    	return item;
    }
}