package fi.publishertools.kss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kss.processing")
public class ProcessingProperties {

    private Phases phases = new Phases();
    private String phaseThreadPrefix = "processing-phase-";

    public Phases getPhases() {
        return phases;
    }

    public void setPhases(Phases phases) {
        this.phases = phases;
    }

    public String getPhaseThreadPrefix() {
        return phaseThreadPrefix;
    }

    public void setPhaseThreadPrefix(String phaseThreadPrefix) {
        this.phaseThreadPrefix = phaseThreadPrefix;
    }

    public static class Phases {
        private int count = 3;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
