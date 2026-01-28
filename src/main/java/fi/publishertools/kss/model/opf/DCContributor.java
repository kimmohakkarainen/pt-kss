package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "contributor", namespace = "http://purl.org/dc/elements/1.1/")
public class DCContributor extends MetaItemBase {

    @XmlAttribute(name = "dir")
    private String dir;

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    private String xmlLang;

    @XmlAttribute(name = "role", namespace = "http://www.idpf.org/2007/opf")
    private String role;

    @XmlAttribute(name = "file-as", namespace = "http://www.idpf.org/2007/opf")
    private String fileAs;

    public DCContributor() {
	super("dc:contributor");
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

    public String getRole() {
	return role;
    }
    
    public String getFileAs() {
	return fileAs;
    }

}
