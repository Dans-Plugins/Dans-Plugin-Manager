package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.ChannelStore;
import dansplugins.dpm.install.ports.ChannelStoreContract;

import java.nio.file.Path;

class ChannelRepositoryContractTest extends ChannelStoreContract {
    @Override
    protected ChannelStore emptyStore(Path dir) {
        return new ChannelRepository(dir.resolve("dpm-channels.properties").toFile(), null);
    }
}
