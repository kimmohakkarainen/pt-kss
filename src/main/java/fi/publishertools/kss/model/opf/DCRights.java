package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "rights", namespace = "http://purl.org/dc/elements/1.1/")
public class DCRights extends MetaItemBase {

    @XmlAttribute(name = "dir")
    private String dir;

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    private String xmlLang;

    public DCRights() {
	super("dc:rights");
    }

    public String getDir() {
        return dir;
    }

    public String getId() {
        return id;
    }

    public String getXmlLang() {
        return xmlLang;
    }
}
