package dansplugins.dpm.install.ports;

import dansplugins.dpm.objects.ReleaseChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Contract every ChannelStore must meet. */
public abstract class ChannelStoreContract {

    protected abstract ChannelStore emptyStore(Path dir);

    // -------------------------------------------------------------------------
    // getChannel() / setChannel() / removeChannel()
    // -------------------------------------------------------------------------

    @Test
    void getChannel_defaultsToStable(@TempDir Path tempDir) {
        assertEquals(ReleaseChannel.STABLE, emptyStore(tempDir).getChannel("Herald"));
    }

    @Test
    void setChannel_pinsThePluginIgnoringCase(@TempDir Path tempDir) {
        ChannelStore store = emptyStore(tempDir);
        store.setChannel("Herald", ReleaseChannel.EXPERIMENTAL);
        assertEquals(ReleaseChannel.EXPERIMENTAL, store.getChannel("herald"));
    }

    @Test
    void removeChannel_returnsThePluginToStable(@TempDir Path tempDir) {
        ChannelStore store = emptyStore(tempDir);
        store.setChannel("Herald", ReleaseChannel.EXPERIMENTAL);
        store.removeChannel("HERALD");
        assertEquals(ReleaseChannel.STABLE, store.getChannel("Herald"));
    }
}
