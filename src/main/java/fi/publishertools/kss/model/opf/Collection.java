package fi.publishertools.kss.model.opf;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "collection", namespace = "http://www.idpf.org/2007/opf")
@XmlType(name = "collection", namespace = "http://www.idpf.org/2007/opf")
public class Collection {

    @XmlAttribute(name = "role")
    private String role;

    @XmlElement(name = "metadata", namespace = "http://www.idpf.org/2007/opf")
    private CollectionMetadata metadata;

    public String getRole() {
        return role;
    }

    public CollectionMetadata getMetadata() {
        return metadata;
    }

    @XmlType(name = "collectionMetadata", namespace = "http://www.idpf.org/2007/opf")
    public static class CollectionMetadata {
        
        @XmlElement(name = "title", namespace = "http://purl.org/dc/elements/1.1/")
        private String title;

        public String getTitle() {
            return title;
        }
    }
} 