package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "identifier", namespace = "http://purl.org/dc/elements/1.1/")
public class DCIdentifier extends MetaItemBase {

    @XmlAttribute(name = "id")
    private String id;
    
    @XmlAttribute(name = "scheme", namespace = "http://www.idpf.org/2007/opf")
    private String scheme;

    public DCIdentifier() {
	super("dc:identifier");
    }
    
    public String getId() {
        return id;
    }
    
    public String getOpfScheme() {
	return scheme;
    }
    
    public static DCIdentifier create(String id, String value) {
    	DCIdentifier target = new DCIdentifier();
    	target.id = id;
    	target.value = value;
    	return target;
    }
}
