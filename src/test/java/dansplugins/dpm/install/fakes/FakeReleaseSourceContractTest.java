package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.ReleaseSource;
import dansplugins.dpm.install.ports.ReleaseSourceContract;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;

class FakeReleaseSourceContractTest extends ReleaseSourceContract {
    @Override
    protected ReleaseSource arrange() {
        return new FakeReleaseSource()
                .publish("Dans-Plugins", "Published", ReleaseChannel.STABLE,
                        new ReleaseInfo("v1.0.0", "http://example.invalid/Published.jar"))
                .publish("Dans-Plugins", "NoJar", ReleaseChannel.STABLE, new ReleaseInfo("v1.0.0", null));
    }
}
