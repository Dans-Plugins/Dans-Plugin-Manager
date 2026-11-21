package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.DebugLog;
import dansplugins.dpm.install.ports.DebugLogContract;

class FakeDebugLogContractTest extends DebugLogContract {
    @Override
    protected DebugLog debugLog() {
        return new FakeDebugLog();
    }
}
