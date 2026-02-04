package fi.publishertools.kss.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.ProcessingStatus;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.AssembleEpubPhase;
import fi.publishertools.kss.phases.CheckMandatoryInformationPhase;
import fi.publishertools.kss.phases.CreatePackageOpfPhase;
import fi.publishertools.kss.phases.ExtractChaptersPhase;
import fi.publishertools.kss.phases.ExtractStoriesPhase;
import fi.publishertools.kss.phases.ImageExtractionPhase;
import fi.publishertools.kss.phases.FinalizationPhase;
import fi.publishertools.kss.phases.GenerateTOCPhase;
import fi.publishertools.kss.phases.GenerateXHTMLPhase;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.processing.ProcessingPipeline;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service that manages the processing pipeline lifecycle and submits files for processing.
 */
@Service
public class ProcessingPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingPipelineService.class);
    private static final String PHASE_THREAD_PREFIX = "phase-";
    private static final int CHECK_MANDATORY_PHASE_INDEX = 3;

    private final ProcessingStatusStore statusStore;
    private final ProcessedResultStore resultStore;
    private final PendingMetadataStore pendingMetadataStore;
    private ProcessingPipeline pipeline;

    public ProcessingPipelineService(ProcessingStatusStore statusStore,
                                     ProcessedResultStore resultStore,
                                     PendingMetadataStore pendingMetadataStore) {
        this.statusStore = statusStore;
        this.resultStore = resultStore;
        this.pendingMetadataStore = pendingMetadataStore;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing processing pipeline service");
        List<ProcessingPhase> phases = createPhases();
        pipeline = new ProcessingPipeline(
                phases,
                statusStore,
                resultStore,
                pendingMetadataStore,
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

    /**
     * Re-queue a ProcessingContext for CheckMandatoryInformationPhase (e.g. after user has filled metadata).
     */
    public void resubmitForMandatoryCheck(ProcessingContext context) {
        if (pipeline == null) {
            logger.error("Pipeline not initialized, cannot resubmit file {} for mandatory check", context.getFileId());
            return;
        }
        try {
            statusStore.setStatus(context.getFileId(), ProcessingStatus.IN_PROGRESS);
            pipeline.submitToPhase(CHECK_MANDATORY_PHASE_INDEX, context);
            logger.info("Resubmitted file {} for mandatory metadata check", context.getFileId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while resubmitting file {} for mandatory check", context.getFileId(), e);
        } catch (Exception e) {
            logger.error("Failed to resubmit file {} for mandatory check", context.getFileId(), e);
        }
    }

    private List<ProcessingPhase> createPhases() {
        List<ProcessingPhase> phases = new ArrayList<>();
        phases.add(new ExtractStoriesPhase());
        phases.add(new ExtractChaptersPhase());
        phases.add(new ImageExtractionPhase());
        phases.add(new CheckMandatoryInformationPhase());
        phases.add(new GenerateXHTMLPhase());
        phases.add(new GenerateTOCPhase());
        phases.add(new CreatePackageOpfPhase());
        phases.add(new AssembleEpubPhase());
        phases.add(new FinalizationPhase());
        logger.info("Created {} processing phases", phases.size());
        return phases;
    }
}
