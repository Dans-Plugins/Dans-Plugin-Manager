package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.VersionStore;
import dansplugins.dpm.install.ports.VersionStoreContract;

import java.nio.file.Path;

class FakeVersionStoreContractTest extends VersionStoreContract {
    @Override
    protected VersionStore emptyStore(Path dir) {
        return new FakeVersionStore();
    }
}
