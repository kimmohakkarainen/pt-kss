package fi.publishertools.kss.phases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
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
        		.setUniqueIdentifier("primary-title")
        		.addSpineItem("koottu-1", "Koottu-1.xhtml", "application/xhtml+xml", null, false)
        		.build();
        context.setPackageObf(opfBytes);

        logger.debug("Created package.opf ({} bytes) for file {}", opfBytes.length, context.getFileId());
    }

}

