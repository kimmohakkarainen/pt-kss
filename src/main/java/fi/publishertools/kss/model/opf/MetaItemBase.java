package fi.publishertools.kss.model.opf;

import java.util.UUID;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "base", namespace = "http://www.idpf.org/2007/opf")
@XmlSeeAlso({ DCContributor.class, DCCreator.class, DCDate.class, DCLanguage.class, DCPublisher.class, DCRights.class,
        DCSubject.class, DCTitle.class, DCType.class, DCIdentifier.class, MetaItem.class, LinkItem.class })
@XmlAccessorType(XmlAccessType.FIELD)
public class MetaItemBase {

    @XmlTransient
    protected String json_id = UUID.randomUUID().toString();

    @XmlTransient
    protected String json_label = "";

    @XmlValue
    protected String value;

    public MetaItemBase() {

    }

    public MetaItemBase(String json_label) {
        this.json_label = json_label;
    }

    public String getValue() {
        return value;
    }

    public MetaItemBase setValue(String value) {
        this.value = value;
        return this;
    }

}
