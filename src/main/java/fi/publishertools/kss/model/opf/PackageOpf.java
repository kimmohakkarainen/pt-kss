package fi.publishertools.kss.model.opf;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "package", namespace = "http://www.idpf.org/2007/opf")
@XmlAccessorType(XmlAccessType.FIELD)
public class PackageOpf {

	@XmlAttribute(name = "version")
	private String version;

	@XmlAttribute(name = "unique-identifier")
	private String uniqueIdentifier;

	@XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
	private String xmlLang;

	@XmlElement(name = "metadata", namespace = "http://www.idpf.org/2007/opf")
	private Metadata metadata;

	@XmlElement(name = "manifest", namespace = "http://www.idpf.org/2007/opf")
	private Manifest manifest;

	@XmlElement(name = "spine", required = true)
	private Spine spine;

	@XmlElement(name = "collection", namespace = "http://www.idpf.org/2007/opf")
	private Collection collection;

	@XmlType(name = "metadata", namespace = "http://www.idpf.org/2007/opf")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Metadata {

		@XmlAnyElement(lax = true)
		private List<MetaItemBase> items;

	}

	@XmlType(name = "manifest", namespace = "http://www.idpf.org/2007/opf")
	public static class Manifest {

		@XmlElement(name = "item")
		private List<ManifestItem> items;

	}

	@XmlType(name = "spine")
	public static class Spine {

		@XmlAttribute(name = "toc")
		private String toc;

		@XmlElement(name = "itemref")
		private List<SpineItem> itemrefs;

	}
	
	public static class Builder {
		
		public static byte[] create() throws JAXBException {
			
			PackageOpf target = new PackageOpf();
			target.metadata = new Metadata();
			target.metadata.items = new ArrayList<>();
			target.manifest = new Manifest();
			target.manifest.items = new ArrayList<>();
			target.spine = new Spine();
			target.spine.itemrefs = new ArrayList<>();
			
			JAXBContext context = JAXBContext.newInstance(PackageOpf.class, ManifestItem.class, MetaItemBase.class,
					SpineItem.class, LinkItem.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			ByteArrayOutputStream outs = new ByteArrayOutputStream();
			marshaller.marshal(target, outs);

			return outs.toByteArray();

		}
	}


}
