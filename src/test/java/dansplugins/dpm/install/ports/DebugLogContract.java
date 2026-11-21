package dansplugins.dpm.install.ports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Contract every DebugLog must meet. */
public abstract class DebugLogContract {

    protected abstract DebugLog debugLog();

    // -------------------------------------------------------------------------
    // warn()
    // -------------------------------------------------------------------------

    @Test
    void warn_neverThrows() {
        assertDoesNotThrow(() -> debugLog().warn("something went wrong"));
    }
}
