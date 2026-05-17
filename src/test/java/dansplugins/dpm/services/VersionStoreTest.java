package dansplugins.dpm.services;

import dansplugins.dpm.utils.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VersionStoreTest {

    // -------------------------------------------------------------------------
    // getStoredTag / setTag
    // -------------------------------------------------------------------------

    @Test
    void setTag_thenGetStoredTag_returnsTag(@TempDir Path tempDir) {
        VersionStore store = store(tempDir);
        store.setTag("medievalfactions", "v4.6.3");
        assertEquals("v4.6.3", store.getStoredTag("medievalfactions"));
    }

    @Test
    void getStoredTag_returnsNullForUnknownPlugin(@TempDir Path tempDir) {
        assertNull(store(tempDir).getStoredTag("notregistered"));
    }

    @Test
    void setTag_keyIsCaseInsensitive(@TempDir Path tempDir) {
        VersionStore store = store(tempDir);
        store.setTag("MedievalFactions", "v4.6.3");
        assertEquals("v4.6.3", store.getStoredTag("medievalfactions"));
        assertEquals("v4.6.3", store.getStoredTag("MEDIEVALFACTIONS"));
    }

    @Test
    void setTag_overwritesPreviousValue(@TempDir Path tempDir) {
        VersionStore store = store(tempDir);
        store.setTag("medievalfactions", "v4.6.2");
        store.setTag("medievalfactions", "v4.6.3");
        assertEquals("v4.6.3", store.getStoredTag("medievalfactions"));
    }

    // -------------------------------------------------------------------------
    // removeTag
    // -------------------------------------------------------------------------

    @Test
    void removeTag_erasesStoredValue(@TempDir Path tempDir) {
        VersionStore store = store(tempDir);
        store.setTag("medievalfactions", "v4.6.3");
        store.removeTag("medievalfactions");
        assertNull(store.getStoredTag("medievalfactions"));
    }

    @Test
    void removeTag_onUnknownPluginIsHarmless(@TempDir Path tempDir) {
        assertDoesNotThrow(() -> store(tempDir).removeTag("notregistered"));
    }

    // -------------------------------------------------------------------------
    // load failure warning
    // -------------------------------------------------------------------------

    @Test
    void load_emitsWarn_whenStoreFileIsUnreadable(@TempDir Path tempDir) {
        // A directory at the expected file path causes FileInputStream to throw
        File notAFile = tempDir.resolve("dpm-versions.properties").toFile();
        notAFile.mkdir();

        List<String> warnings = new ArrayList<>();
        Logger capturing = new Logger(null) {
            @Override public void log(String m) {}
            @Override public void warn(String m) { warnings.add(m); }
        };
        new VersionStore(notAFile, capturing);
        assertFalse(warnings.isEmpty(), "load failure must emit a warn");
        assertTrue(warnings.get(0).contains("version store"), "warn must mention version store");
    }

    // -------------------------------------------------------------------------
    // persistence across instances
    // -------------------------------------------------------------------------

    @Test
    void tagsPersistedToDisk_survivesNewInstance(@TempDir Path tempDir) {
        File storeFile = tempDir.resolve("dpm-versions.properties").toFile();

        new VersionStore(storeFile, noOpLogger()).setTag("currencies", "v2.1.0");

        assertEquals("v2.1.0", new VersionStore(storeFile, noOpLogger()).getStoredTag("currencies"));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private VersionStore store(Path tempDir) {
        return new VersionStore(tempDir.resolve("dpm-versions.properties").toFile(), noOpLogger());
    }

    private Logger noOpLogger() {
        return new Logger(null) {
            @Override public void log(String m) {}
            @Override public void warn(String m) {}
        };
    }
}
