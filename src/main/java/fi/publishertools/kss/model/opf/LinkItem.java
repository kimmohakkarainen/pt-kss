package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "link", namespace = "http://www.idpf.org/2007/opf")
public class LinkItem extends MetaItemBase {

    @XmlAttribute(name = "rel")
    private String rel;

    @XmlAttribute(name = "href")
    private String href;

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "refines")
    private String refines;

    @XmlAttribute(name = "media-type")
    private String mediaType;

    @XmlAttribute(name = "properties")
    private String properties;

    public LinkItem() {
        super("link");
    }

    public String getRel() {
        return rel;
    }

    public String getHref() {
        return href;
    }

    public String getId() {
        return id;
    }

    public String getRefines() {
        return refines;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getProperties() {
        return properties;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRefines(String refines) {
        this.refines = refines;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

}
