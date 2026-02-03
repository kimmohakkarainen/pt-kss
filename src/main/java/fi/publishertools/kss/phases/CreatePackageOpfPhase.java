package fi.publishertools.kss.phases;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.opf.DCCreator;
import fi.publishertools.kss.model.opf.DCIdentifier;
import fi.publishertools.kss.model.opf.DCLanguage;
import fi.publishertools.kss.model.opf.DCPublisher;
import fi.publishertools.kss.model.opf.DCTitle;
import fi.publishertools.kss.model.opf.MetaItem;
import fi.publishertools.kss.model.opf.PackageOpf;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Creates a minimal EPUB 3 {@code package.opf} XML document and stores it
 * as UTF-8 bytes into {@link ProcessingContext} metadata under key {@code "packageOpf"}.
 * <p>
 * The document:
 * <ul>
 *     <li>Uses the {@code http://www.idpf.org/2007/opf} namespace</li>
 *     <li>Has root element {@code &lt;package&gt;} with version 3.0</li>
 *     <li>Contains minimal, mostly empty {@code &lt;metadata&gt;} and {@code &lt;manifest&gt;} elements</li>
 * </ul>
 */
public class CreatePackageOpfPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(CreatePackageOpfPhase.class);

    @Override
    public void process(ProcessingContext context) throws Exception {
    	
        logger.debug("Creating package.opf for file {}", context.getFileId());

        byte [] opfBytes = PackageOpf.Builder
        		.title(context.getOriginalFilename())
        		.setVersion("3.0")
        		.setXmlLang("FI")
        		.setUniqueIdentifier("primary-identifier")
        		.addMetaItem(DCIdentifier.create("primary-identifier", context.getMetadata("identifier", String.class)))
        		.addMetaItem(DCTitle.create(context.getMetadata("title", String.class)))
        		.addMetaItem(DCCreator.create(context.getMetadata("creator", String.class)))
        		.addMetaItem(DCPublisher.create(context.getMetadata("publisher", String.class)))
        		.addMetaItem(DCLanguage.create(context.getMetadata("language", String.class)))
        		.addMetaItem(MetaItem.createProperty("dcterms:modified", LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'"))))
				.addManifestItem("toc", "toc.xhtml", "application/xhtml+xml", "nav")
        		.addSpineItem("koottu-1", "Koottu-1.xhtml", "application/xhtml+xml", null, false)
        		.build();
        context.setPackageObf(opfBytes);

        logger.debug("Created package.opf ({} bytes) for file {}", opfBytes.length, context.getFileId());
    }

}

