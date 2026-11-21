package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.ReleaseSource;
import dansplugins.dpm.install.ports.ReleaseSourceContract;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.utils.Logger;

// The HTTP exchange itself is covered by GitHubReleaseRepositoryTest; this pins the port contract
// on top of doFetch, the same seam those tests use.
class GitHubReleaseRepositoryContractTest extends ReleaseSourceContract {
    @Override
    protected ReleaseSource arrange() {
        Logger quiet = new Logger(null) {
            @Override public void log(String m) {}
            @Override public void warn(String m) {}
        };
        return new GitHubReleaseRepository(quiet) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                if ("Published".equals(repo)) return new ReleaseInfo("v1.0.0", "http://example.invalid/Published.jar");
                if ("NoJar".equals(repo)) return new ReleaseInfo("v1.0.0", null);
                return ReleaseInfo.NO_RELEASE;
            }

            @Override
            ReleaseInfo doFetchExperimental(String owner, String repo) {
                return ReleaseInfo.NO_RELEASE;
            }
        };
    }
}
