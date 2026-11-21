package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.VersionStore;
import dansplugins.dpm.install.ports.VersionStoreContract;

import java.nio.file.Path;

class VersionRepositoryContractTest extends VersionStoreContract {
    @Override
    protected VersionStore emptyStore(Path dir) {
        return new VersionRepository(dir.resolve("dpm-versions.properties").toFile(), null);
    }
}
