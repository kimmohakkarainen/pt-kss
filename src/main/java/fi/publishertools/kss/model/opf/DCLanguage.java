package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "language", namespace = "http://purl.org/dc/elements/1.1/")
public class DCLanguage extends MetaItemBase {

    @XmlAttribute(name = "id")
    private String id;

    public DCLanguage() {
	super("dc:language");
    }
    
    public String getId() {
        return id;
    }

    public static DCLanguage create(String value) {
    	DCLanguage target = new DCLanguage();
    	target.value = value;
    	return target;
    }
}
