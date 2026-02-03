package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "meta", namespace = "http://www.idpf.org/2007/opf")
@XmlAccessorType(XmlAccessType.FIELD)
public class MetaItem extends MetaItemBase {

    @XmlAttribute(name = "dir")
    private String dir;

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "property")
    private String property;

    @XmlAttribute(name = "refines")
    private String refines;

    @XmlAttribute(name = "scheme")
    private String scheme;

    @XmlAttribute(name = "lang", namespace = "http://purl.org/dc/elements/1.1/")
    private String xmlLang;

    @XmlAttribute(name = "name")
    protected String name;

    @XmlAttribute(name = "content")
    protected String content;
    
    public static MetaItem create(String name, String content) {
    	MetaItem item = new MetaItem();
    	item.name = name;
    	item.content = content;
    	return item;
    }
    
    public static MetaItem createProperty(String property, String value) {
    	MetaItem item = new MetaItem();
    	item.property = property;
    	item.value = value;
    	return item;
    }
    
    public MetaItem() {
	super("meta");
    }
    

    public String getDir() {
        return dir;
    }

    public String getId() {
        return id;
    }
    
    public String getProperty() {
        return property;
    }

    public String getRefines() {
        return refines;
    }

    public String getScheme() {
        return scheme;
    }

    public String getXmlLang() {
        return xmlLang;
    }
    
    public String getName() {
	return name;
    }
    
    public String getContent() {
	return content;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setRefines(String refines) {
        this.refines = refines;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void setXmlLang(String xmlLang) {
        this.xmlLang = xmlLang;
    }

    public void setName(String name) {
	this.name = name;
    }
    
    public void setContent(String content) {
	this.content = content;
    }
}