package dansplugins.dpm.install.ports;

import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;

/**
 * Where a plugin's newest downloadable release comes from.
 *
 * <p>{@link ReleaseInfo} is the existing in-repo contract and is used as-is.
 *
 * <p>Returns {@link ReleaseInfo#NO_RELEASE} (compared by identity) when the repository publishes
 * nothing on that channel, {@code null} when the source could not be reached or the release has
 * no usable {@code .jar} asset, and otherwise a release with a non-null jar URL. Never throws for
 * network trouble. May block on the network, so call it off the server's main thread.
 * Implementations must be safe to call from several threads at once.
 */
public interface ReleaseSource {
    ReleaseInfo getRelease(String owner, String repo, ReleaseChannel channel);
}
