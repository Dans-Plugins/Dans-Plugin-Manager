package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.ChannelStore;
import dansplugins.dpm.install.ports.ChannelStoreContract;

import java.nio.file.Path;

class FakeChannelStoreContractTest extends ChannelStoreContract {
    @Override
    protected ChannelStore emptyStore(Path dir) {
        return new FakeChannelStore();
    }
}
