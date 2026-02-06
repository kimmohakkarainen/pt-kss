package fi.publishertools.kss.phases;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.processing.ProcessingPhase;

/**
 * Phase 3: Finalize output by adding finalization timestamp and preparing final JSON structure.
 */
public class FinalizationPhase extends ProcessingPhase {

    private static final Logger logger = LoggerFactory.getLogger(FinalizationPhase.class);

    @Override
    public void process(ProcessingContext context) throws Exception {
        logger.debug("Finalizing output for file {}", context.getFileId());

        // Add finalization timestamp
        context.addMetadata("finalizedAt", Instant.now().toString());

        // Mark as finalized
        context.addMetadata("status", "finalized");

        logger.debug("Finalization completed for file {}", context.getFileId());
    }

}
