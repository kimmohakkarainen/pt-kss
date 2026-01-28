package fi.publishertools.kss.model.container;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "container")
public class ContainerXml {

    @XmlAttribute(name = "version", required = true)
    private String version;

    @XmlElement(name = "rootfiles", required = true)
    private RootFiles rootfiles = new RootFiles();

    public static byte[] create(List<String> rootfiles) throws JAXBException {
        ContainerXml target = new ContainerXml();
        target.version = "1.0";
        target.rootfiles = RootFiles.create(rootfiles);

        JAXBContext containerContext = JAXBContext.newInstance(ContainerXml.class);
        Marshaller containerMarshaller = containerContext.createMarshaller();

        ByteArrayOutputStream outs = new ByteArrayOutputStream();
        containerMarshaller.marshal(target, outs);

        return outs.toByteArray();
    }

    public String getVersion() {
        return this.version;
    }

    public List<RootFile> getRootFiles() {
        return this.rootfiles.getRootFiles();
    }

    @XmlType(name = "rootfiles")
    public static class RootFiles {

        public static RootFiles create(List<String> files) {
            RootFiles target = new RootFiles();
            target.rootfiles = files.stream().map(rf -> RootFile.create(rf, "application/oebps-package+xml")).toList();
            return target;
        }

        @XmlElement(name = "rootfile", required = true)
        private List<RootFile> rootfiles = new ArrayList<>();

        public List<RootFile> getRootFiles() {
            return this.rootfiles;
        }
    }
}
