package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "itemref", namespace = "http://www.idpf.org/2007/opf")
public class SpineItem {

    @XmlAttribute(name = "idref")
    private String idRef;

    @XmlAttribute(name = "linear")
    private String linear;

    public String getIdRef() {
        return idRef;
    }

    public String getLinear() {
        return linear;
    }
}