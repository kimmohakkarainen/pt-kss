package fi.publishertools.kss.processing.phases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.publishertools.kss.model.ProcessingContext;
import fi.publishertools.kss.model.serialization.ProcessingContextSerializer;
import fi.publishertools.kss.phases.A4_ResolveContentHierarchy;

/**
 * Loads serialized ProcessingContext from disk and runs A4_ResolveContentHierarchy.
 * Used for development input for A4-phase and as a main routine for running A4 in the debugger.
 * <p>
 * Path resolution: program arg → env KSS_A3_OUTPUT_PATH → default target/a3-context.ser
 */
class A4_ResolveContentHierarchyFromFileTest {

    private static final String KSS_A3_OUTPUT_PATH_ENV = "./a3-context.object";

    @Test
    @DisplayName("Runs A4 from serialized ProcessingContext when file exists")
    void runsA4FromSerializedContext() throws Exception {
        Path path = resolvePath(KSS_A3_OUTPUT_PATH_ENV);
        assumeTrue(Files.exists(path), "Serialized context file not found at " + path + ". Run pipeline with KSS_A3_OUTPUT_PATH set to produce it.");

        ProcessingContext context = ProcessingContextSerializer.deserialize(path);
        A4_ResolveContentHierarchy phase = new A4_ResolveContentHierarchy();
        phase.process(context);

        assertThat(context.getChapters()).isNotNull();
    }

    /**
     * Main entry point for running A4 from serialized context in the debugger.
     * Usage: pass path as first arg, or set KSS_A3_OUTPUT_PATH env var, or use default target/a3-context.ser.
     */
    public static void main(String[] args) {
        try {
            Path path = resolvePath(KSS_A3_OUTPUT_PATH_ENV);
            if (!Files.exists(path)) {
                System.err.println("File not found: " + path.toAbsolutePath());
                System.err.println("Run the pipeline with KSS_A3_OUTPUT_PATH=" + path + " to produce the serialized context.");
                System.exit(1);
            }

            ProcessingContext context = ProcessingContextSerializer.deserialize(path);
            A4_ResolveContentHierarchy phase = new A4_ResolveContentHierarchy();
            phase.process(context);

            System.out.println("A4 completed. Chapters: " + (context.getChapters() != null ? context.getChapters().size() : 0));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Path resolvePath(String argPath) {
        if (argPath != null && !argPath.isEmpty()) {
            return Path.of(argPath);
        }
        return Path.of(KSS_A3_OUTPUT_PATH_ENV);
    }
}
