package fi.publishertools.kss.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.publishertools.kss.model.MandatoryMetadataMissingException;
import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.ProcessingStatus;
import fi.publishertools.kss.service.PendingMetadataStore;
import fi.publishertools.kss.service.ProcessedResultStore;
import fi.publishertools.kss.service.ProcessingStatusStore;

/**
 * Orchestrates multiple processing phases, each running on its own thread.
 */
public class ProcessingPipeline {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingPipeline.class);

    private final List<ProcessingPhase> phases;
    private final List<BlockingQueue<ProcessingContext>> buffers;
    private final List<Thread> workerThreads;
    private final ProcessingStatusStore statusStore;
    private final ProcessedResultStore resultStore;
    private final PendingMetadataStore pendingMetadataStore;
    private final String threadPrefix;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ProcessingPipeline(List<ProcessingPhase> phases,
                              ProcessingStatusStore statusStore,
                              ProcessedResultStore resultStore,
                              PendingMetadataStore pendingMetadataStore,
                              String threadPrefix) {
        this.phases = new ArrayList<>(phases);
        this.statusStore = statusStore;
        this.resultStore = resultStore;
        this.pendingMetadataStore = pendingMetadataStore;
        this.threadPrefix = threadPrefix;
        this.buffers = new ArrayList<>();
        this.workerThreads = new ArrayList<>();

        // Create buffers: one for each phase's input
        for (int i = 0; i < phases.size(); i++) {
            buffers.add(new LinkedBlockingQueue<>());
        }
    }

    /**
     * Start all worker threads for the phases.
     */
    public void start() {
        if (running.get()) {
            logger.warn("Pipeline is already running");
            return;
        }

        running.set(true);
        logger.info("Starting processing pipeline with {} phases", phases.size());

        for (int i = 0; i < phases.size(); i++) {
            final int phaseIndex = i;
            final ProcessingPhase phase = phases.get(phaseIndex);
            final BlockingQueue<ProcessingContext> inputBuffer = buffers.get(phaseIndex);
            final BlockingQueue<ProcessingContext> outputBuffer = (phaseIndex < phases.size() - 1)
                    ? buffers.get(phaseIndex + 1)
                    : null; // Last phase has no output buffer
            final String threadName = threadPrefix + (phaseIndex + 1);

            Thread workerThread = new Thread(() -> {
                Thread.currentThread().setName(threadName);
                logger.info("Phase {} worker thread started", phase.getName());

                while (running.get() || !inputBuffer.isEmpty()) {
                    try {
                        ProcessingContext context = inputBuffer.take();
                        logger.debug("Phase {} processing file {}", phase.getName(), context.getFileId());

                        try {
                            // Set status to IN_PROGRESS if this is the first phase
                            if (phaseIndex == 0) {
                                statusStore.setStatus(context.getFileId(), ProcessingStatus.IN_PROGRESS);
                            }

                            // Process the context
                            phase.process(context);

                            // Pass to next phase or store final result
                            if (outputBuffer != null) {
                                outputBuffer.put(context);
                                logger.debug("Phase {} completed, passed to next phase", phase.getName());
                            } else {
                                // Last phase: store final result
                                storeFinalResult(context);
                                logger.info("Processing completed for file {}", context.getFileId());
                            }
                        } catch (MandatoryMetadataMissingException e) {
                            statusStore.setStatus(e.getContext().getFileId(), ProcessingStatus.AWAITING_METADATA);
                            pendingMetadataStore.store(e.getContext().getFileId(), e.getContext());
                            logger.info("File {} awaiting mandatory metadata", e.getContext().getFileId());
                        } catch (Exception e) {
                            logger.error("Error in phase {} processing file {}", phase.getName(), context.getFileId(), e);
                            statusStore.setStatus(context.getFileId(), ProcessingStatus.ERROR);
                            resultStore.storeError(context.getFileId(), e.getMessage());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.info("Phase {} worker thread interrupted", phase.getName());
                        break;
                    }
                }

                logger.info("Phase {} worker thread stopped", phase.getName());
            }, threadName);

            workerThread.setDaemon(false);
            workerThread.start();
            workerThreads.add(workerThread);
        }

        logger.info("Processing pipeline started with {} worker threads", workerThreads.size());
    }

    /**
     * Stop all worker threads gracefully.
     */
    public void stop() {
        if (!running.get()) {
            return;
        }

        logger.info("Stopping processing pipeline");
        running.set(false);

        // Interrupt all worker threads
        for (Thread thread : workerThreads) {
            thread.interrupt();
        }

        // Wait for threads to finish
        for (Thread thread : workerThreads) {
            try {
                thread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for thread {}", thread.getName());
            }
        }

        logger.info("Processing pipeline stopped");
    }

    /**
     * Submit a file for processing by placing it in the first phase's buffer.
     */
    public void submit(ProcessingContext context) throws InterruptedException {
        if (!running.get()) {
            throw new IllegalStateException("Pipeline is not running");
        }
        buffers.get(0).put(context);
        logger.debug("Submitted file {} to pipeline", context.getFileId());
    }

    /**
     * Submit a context to a specific phase's input buffer (e.g. for re-queue after metadata approval).
     */
    public void submitToPhase(int phaseIndex, ProcessingContext context) throws InterruptedException {
        if (!running.get()) {
            throw new IllegalStateException("Pipeline is not running");
        }
        if (phaseIndex < 0 || phaseIndex >= buffers.size()) {
            throw new IllegalArgumentException("Invalid phase index: " + phaseIndex);
        }
        buffers.get(phaseIndex).put(context);
        logger.debug("Submitted file {} to phase {}", context.getFileId(), phaseIndex);
    }

    private void storeFinalResult(ProcessingContext context) {
        // Convert context metadata to final payload
        java.util.Map<String, Object> payload = new java.util.HashMap<>(context.getMetadata());
        payload.put("fileId", context.getFileId());
        payload.put("originalFilename", context.getOriginalFilename());
        payload.put("contentType", context.getContentType());
        payload.put("fileSize", context.getFileSize());
        payload.put("uploadTime", context.getUploadTime().toString());
        payload.put("storiesList", context.getStoriesList());
        payload.put("chapters", context.getChapters());
        payload.put("imageList", context.getImageList());
        payload.put("xhtml", context.getXhtmlContent());

        byte[] epubFile = context.getMetadata("epubFile", byte[].class);
        if (epubFile != null) {
            payload.put("epubFile", epubFile);
        }

        statusStore.setStatus(context.getFileId(), ProcessingStatus.READY);
        resultStore.storeResult(context.getFileId(), payload);
    }
}
