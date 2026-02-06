package fi.publishertools.kss.processing;

import java.io.IOException;

import fi.publishertools.kss.model.ProcessingContext;

/**
 * Interface for processing phases in the pipeline.
 * Each phase processes a ProcessingContext and can modify it or add metadata.
 */
public abstract class ProcessingPhase {

    /**
     * Process the given context. The context can be modified in place.
     *
     * @param context the processing context containing file data and metadata
     * @throws Exception if processing fails
     */
    public abstract void process(ProcessingContext context) throws Exception, IOException;

    /**
     * Get the phase name for logging and identification.
     *
     * @return the phase name
     */
    public String getName() {
    	return this.getClass().getSimpleName();
    }
}
