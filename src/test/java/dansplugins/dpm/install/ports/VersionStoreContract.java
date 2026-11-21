package dansplugins.dpm.install.ports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Contract every VersionStore must meet. */
public abstract class VersionStoreContract {

    protected abstract VersionStore emptyStore(Path dir);

    // -------------------------------------------------------------------------
    // getStoredTag() / setTag()
    // -------------------------------------------------------------------------

    @Test
    void getStoredTag_returnsNullForAnUnknownPlugin(@TempDir Path tempDir) {
        assertNull(emptyStore(tempDir).getStoredTag("Herald"));
    }

    @Test
    void setTag_isReadBackIgnoringCase(@TempDir Path tempDir) {
        VersionStore store = emptyStore(tempDir);
        store.setTag("Herald", "v1.0.0");
        assertEquals("v1.0.0", store.getStoredTag("HERALD"));
    }

    @Test
    void setTag_replacesThePreviousTag(@TempDir Path tempDir) {
        VersionStore store = emptyStore(tempDir);
        store.setTag("Herald", "v1.0.0");
        store.setTag("herald", "v1.1.0");
        assertEquals("v1.1.0", store.getStoredTag("Herald"));
    }
}
