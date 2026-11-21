package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.ReleaseSource;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;

import java.util.HashMap;
import java.util.Map;

/** In-memory ReleaseSource. Unknown repos are unpublished; {@link #unreachable} ones return null. */
public class FakeReleaseSource implements ReleaseSource {
    private final Map<String, ReleaseInfo> releases = new HashMap<>();
    private final Map<String, Boolean> unreachable = new HashMap<>();
    public int calls;

    public FakeReleaseSource publish(String owner, String repo, ReleaseChannel channel, ReleaseInfo release) {
        releases.put(key(owner, repo, channel), release);
        return this;
    }

    public FakeReleaseSource unreachable(String owner, String repo, ReleaseChannel channel) {
        unreachable.put(key(owner, repo, channel), true);
        return this;
    }

    @Override
    public ReleaseInfo getRelease(String owner, String repo, ReleaseChannel channel) {
        calls++;
        String key = key(owner, repo, channel);
        if (unreachable.containsKey(key)) return null;
        ReleaseInfo release = releases.get(key);
        if (release == null) return ReleaseInfo.NO_RELEASE;
        return release.getJarUrl() == null ? null : release;
    }

    private static String key(String owner, String repo, ReleaseChannel channel) {
        return owner + "/" + repo + "#" + channel;
    }
}
