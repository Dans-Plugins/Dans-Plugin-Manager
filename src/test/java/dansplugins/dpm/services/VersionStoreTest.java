package dansplugins.dpm.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

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
    // persistence across instances
    // -------------------------------------------------------------------------

    @Test
    void tagsPersistedToDisk_survivesNewInstance(@TempDir Path tempDir) {
        File storeFile = tempDir.resolve("dpm-versions.properties").toFile();

        new VersionStore(storeFile).setTag("currencies", "v2.1.0");

        assertEquals("v2.1.0", new VersionStore(storeFile).getStoredTag("currencies"));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private VersionStore store(Path tempDir) {
        return new VersionStore(tempDir.resolve("dpm-versions.properties").toFile());
    }
}
