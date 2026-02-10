package fi.publishertools.kss.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.StoredFile;
import fi.publishertools.kss.phases.C4_AssembleEPUB;
import fi.publishertools.kss.phases.B1_CheckMandatoryInformation;
import fi.publishertools.kss.phases.B2_ProposeImageAltTexts;
import fi.publishertools.kss.phases.C3_CreatePackageOpf;
import fi.publishertools.kss.phases.A2_ExtractChapters;
import fi.publishertools.kss.phases.A1_ExtractStories;
import fi.publishertools.kss.phases.A3_ExtractImageInfo;
import fi.publishertools.kss.phases.A4_ResolveContentHierarchy;
import fi.publishertools.kss.phases.C5_Finalization;
import fi.publishertools.kss.phases.C2_GenerateTableOfContents;
import fi.publishertools.kss.phases.C1_GenerateXHTML;
import fi.publishertools.kss.processing.ProcessingPhase;
import fi.publishertools.kss.processing.ProcessingPipeline;
import fi.publishertools.kss.processing.ProcessingStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service that manages the processing pipeline lifecycle and submits files for processing.
 */
@Service
public class ProcessingPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingPipelineService.class);
    private static final String PHASE_THREAD_PREFIX = "phase-";
    private static final int CHECK_MANDATORY_PHASE_INDEX = 4;

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
        phases.add(new A1_ExtractStories());
        phases.add(new A2_ExtractChapters());
        phases.add(new A3_ExtractImageInfo());
        phases.add(new A4_ResolveContentHierarchy());
        phases.add(new B1_CheckMandatoryInformation());
        phases.add(new B2_ProposeImageAltTexts());
        phases.add(new C1_GenerateXHTML());
        phases.add(new C2_GenerateTableOfContents());
        phases.add(new C3_CreatePackageOpf());
        phases.add(new C4_AssembleEPUB());
        phases.add(new C5_Finalization());
        logger.info("Created {} processing phases", phases.size());
        return phases;
    }
}
