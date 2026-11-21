package dansplugins.dpm.install.ports;

import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Contract every ReleaseSource must meet. Subclasses arrange one published stable release. */
public abstract class ReleaseSourceContract {

    /**
     * A source where Dans-Plugins/Published has a stable release tagged v1.0.0 whose jar is
     * http://example.invalid/Published.jar, Dans-Plugins/NoJar has a stable release with no jar
     * asset, and nothing else is published on any channel.
     */
    protected abstract ReleaseSource arrange();

    // -------------------------------------------------------------------------
    // getRelease()
    // -------------------------------------------------------------------------

    @Test
    void getRelease_returnsTheReleaseWithItsJarUrl() {
        ReleaseInfo release = arrange().getRelease("Dans-Plugins", "Published", ReleaseChannel.STABLE);
        assertNotNull(release);
        assertEquals("v1.0.0", release.getTagName());
        assertEquals("http://example.invalid/Published.jar", release.getJarUrl());
    }

    @Test
    void getRelease_returnsNoReleaseSentinelWhenNothingIsPublished() {
        assertSame(ReleaseInfo.NO_RELEASE, arrange().getRelease("Dans-Plugins", "Missing", ReleaseChannel.STABLE));
    }

    @Test
    void getRelease_returnsNullWhenTheReleaseHasNoJar() {
        assertNull(arrange().getRelease("Dans-Plugins", "NoJar", ReleaseChannel.STABLE));
    }

    @Test
    void getRelease_keepsChannelsApart() {
        assertSame(ReleaseInfo.NO_RELEASE, arrange().getRelease("Dans-Plugins", "Published", ReleaseChannel.EXPERIMENTAL));
    }
}
