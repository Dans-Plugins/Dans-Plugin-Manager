package dansplugins.dpm.repositories;

import dansplugins.dpm.objects.ReleaseChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChannelRepositoryTest {

    private static ChannelRepository repository(Path tempDir) {
        return new ChannelRepository(new File(tempDir.toFile(), "dpm-channels.properties"), null);
    }

    // -------------------------------------------------------------------------
    // getChannel()
    // -------------------------------------------------------------------------

    @Test
    void getChannel_defaultsToStableForUnknownPlugin(@TempDir Path tempDir) {
        assertEquals(ReleaseChannel.STABLE, repository(tempDir).getChannel("NeverSeen"));
    }

    @Test
    void getChannel_isCaseInsensitiveInPluginName(@TempDir Path tempDir) {
        ChannelRepository repository = repository(tempDir);
        repository.setChannel("MedievalFactions", ReleaseChannel.EXPERIMENTAL);

        assertEquals(ReleaseChannel.EXPERIMENTAL, repository.getChannel("medievalfactions"));
        assertEquals(ReleaseChannel.EXPERIMENTAL, repository.getChannel("MEDIEVALFACTIONS"));
    }

    @Test
    void getChannel_fallsBackToStableForUnrecognizedStoredValue(@TempDir Path tempDir) throws Exception {
        File store = new File(tempDir.toFile(), "dpm-channels.properties");
        Files.writeString(store.toPath(), "myplugin=bananas\n");

        assertEquals(ReleaseChannel.STABLE, new ChannelRepository(store, null).getChannel("MyPlugin"));
    }

    // -------------------------------------------------------------------------
    // setChannel()
    // -------------------------------------------------------------------------

    @Test
    void setChannel_roundTripsThroughANewRepositoryInstance(@TempDir Path tempDir) {
        repository(tempDir).setChannel("MyPlugin", ReleaseChannel.EXPERIMENTAL);

        assertEquals(ReleaseChannel.EXPERIMENTAL, repository(tempDir).getChannel("MyPlugin"),
                "Channel must persist across restarts");
    }

    @Test
    void setChannel_overwritesPreviousChannel(@TempDir Path tempDir) {
        ChannelRepository repository = repository(tempDir);
        repository.setChannel("MyPlugin", ReleaseChannel.EXPERIMENTAL);
        repository.setChannel("MyPlugin", ReleaseChannel.STABLE);

        assertEquals(ReleaseChannel.STABLE, repository.getChannel("MyPlugin"));
    }

    @Test
    void setChannel_doesNotAffectOtherPlugins(@TempDir Path tempDir) {
        ChannelRepository repository = repository(tempDir);
        repository.setChannel("PluginA", ReleaseChannel.EXPERIMENTAL);

        assertEquals(ReleaseChannel.STABLE, repository.getChannel("PluginB"));
    }

    // -------------------------------------------------------------------------
    // removeChannel()
    // -------------------------------------------------------------------------

    @Test
    void removeChannel_returnsPluginToTheStableDefault(@TempDir Path tempDir) {
        ChannelRepository repository = repository(tempDir);
        repository.setChannel("MyPlugin", ReleaseChannel.EXPERIMENTAL);

        repository.removeChannel("MyPlugin");

        assertEquals(ReleaseChannel.STABLE, repository.getChannel("MyPlugin"));
        assertEquals(ReleaseChannel.STABLE, repository(tempDir).getChannel("MyPlugin"),
                "Removal must persist across restarts");
    }

    @Test
    void removeChannel_isSafeForAPluginThatWasNeverPinned(@TempDir Path tempDir) {
        ChannelRepository repository = repository(tempDir);

        assertDoesNotThrow(() -> repository.removeChannel("NeverSeen"));
        assertEquals(ReleaseChannel.STABLE, repository.getChannel("NeverSeen"));
    }

    // -------------------------------------------------------------------------
    // ReleaseChannel.fromStored()
    // -------------------------------------------------------------------------

    @Test
    void fromStored_parsesKnownNamesIgnoringCaseAndWhitespace() {
        assertEquals(ReleaseChannel.EXPERIMENTAL, ReleaseChannel.fromStored("experimental"));
        assertEquals(ReleaseChannel.EXPERIMENTAL, ReleaseChannel.fromStored(" EXPERIMENTAL "));
        assertEquals(ReleaseChannel.STABLE, ReleaseChannel.fromStored("stable"));
    }

    @Test
    void fromStored_defaultsToStableForNullOrUnknown() {
        assertEquals(ReleaseChannel.STABLE, ReleaseChannel.fromStored(null));
        assertEquals(ReleaseChannel.STABLE, ReleaseChannel.fromStored(""));
        assertEquals(ReleaseChannel.STABLE, ReleaseChannel.fromStored("beta"));
    }
}
