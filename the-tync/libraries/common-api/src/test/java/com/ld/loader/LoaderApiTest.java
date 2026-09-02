package com.ld.loader;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the {@link LoaderApi} interface.
 * Exercises every documented contract point through a compliant
 * recording implementation.
 */
class LoaderApiTest {

    /**
     * Minimal in-memory implementation used to exercise the contract.
     */
    private static final class RecordingLoaderApi implements LoaderApi {
        private final List<String> injected = new ArrayList<>();
        private final List<String> initialized = new ArrayList<>();
        private final String supportedVersion;

        RecordingLoaderApi(String supportedVersion) {
            this.supportedVersion = supportedVersion;
        }

        @Override
        public void initialize() {
            initialized.add("init");
        }

        @Override
        public boolean supportsVersion(String version) {
            return supportedVersion.equals(version);
        }

        @Override
        public void injectMod(String modIdentifier) {
            injected.add(modIdentifier);
        }
    }

    @Test
    void compliantImplementationCanInitialize() {
        RecordingLoaderApi loader = new RecordingLoaderApi("1.21");
        loader.initialize();
        assertEquals(List.of("init"), loader.initialized);
    }

    @Test
    void supportsVersionMatchesExactVersion() {
        RecordingLoaderApi loader = new RecordingLoaderApi("1.21");
        assertTrue(loader.supportsVersion("1.21"));
    }

    @Test
    void supportsVersionRejectsOtherVersions() {
        RecordingLoaderApi loader = new RecordingLoaderApi("1.21");
        assertFalse(loader.supportsVersion("1.20.4"));
    }

    @Test
    void supportsVersionTreatsNullAsNonMatchWithoutFailing() {
        RecordingLoaderApi loader = new RecordingLoaderApi("1.21");
        // A well-formed implementation answers false for an unknown
        // version reference rather than throwing.
        assertFalse(loader.supportsVersion(null));
    }

    @Test
    void injectModRecordsIdentifier() {
        RecordingLoaderApi loader = new RecordingLoaderApi("1.21");
        loader.injectMod("mymod-1.0.0.jar");
        assertEquals(List.of("mymod-1.0.0.jar"), loader.injected);
    }

    @Test
    void injectModAcceptsMultipleIdentifiersInOrder() {
        RecordingLoaderApi loader = new RecordingLoaderApi("1.21");
        loader.injectMod("mod-a");
        loader.injectMod("mod-b");
        assertEquals(List.of("mod-a", "mod-b"), loader.injected);
    }
}
