package fi.publishertools.kss.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.processing.ProcessingPipeline;
import fi.publishertools.kss.processing.phases.ExtractChaptersPhase;
import fi.publishertools.kss.processing.phases.ExtractStoriesPhase;
import fi.publishertools.kss.processing.phases.FinalizationPhase;
import fi.publishertools.kss.processing.phases.GenerateXHTMLPhase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service that manages the processing pipeline lifecycle and submits files for processing.
 */
@Service
public class ProcessingPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingPipelineService.class);
    private static final String PHASE_THREAD_PREFIX = "phase-";

    private final ProcessingStatusStore statusStore;
    private final ProcessedResultStore resultStore;
    private ProcessingPipeline pipeline;

    public ProcessingPipelineService(ProcessingStatusStore statusStore,
                                     ProcessedResultStore resultStore) {
        this.statusStore = statusStore;
        this.resultStore = resultStore;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing processing pipeline service");
        List<ProcessingPhase> phases = createPhases();
        pipeline = new ProcessingPipeline(
                phases,
                statusStore,
                resultStore,
                PHASE_THREAD_PREFIX
        );
        pipeline.start();
        logger.info("Processing pipeline service initialized");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down processing pipeline service");
        if (pipeline != null) {
            pipeline.stop();
        }
        logger.info("Processing pipeline service shut down");
    }

    /**
     * Submit a stored file for processing.
     */
    public void submitForProcessing(StoredFile storedFile) {
        if (pipeline == null) {
            logger.error("Pipeline not initialized, cannot submit file {} for processing", storedFile.getId());
            return;
        }
        try {
            ProcessingContext context = new ProcessingContext(storedFile);
            pipeline.submit(context);
            logger.info("Submitted file {} for processing", storedFile.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while submitting file {} for processing", storedFile.getId(), e);
        } catch (Exception e) {
            logger.error("Failed to submit file {} for processing", storedFile.getId(), e);
        }
    }

    private List<ProcessingPhase> createPhases() {
        List<ProcessingPhase> phases = new ArrayList<>();
        phases.add(new ExtractStoriesPhase());
        phases.add(new ExtractChaptersPhase());
        phases.add(new GenerateXHTMLPhase());
        phases.add(new FinalizationPhase());
        logger.info("Created {} processing phases", phases.size());
        return phases;
    }
}
