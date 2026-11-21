package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.Notifier;
import dansplugins.dpm.install.ports.NotifierContract;

class FakeNotifierContractTest extends NotifierContract {
    @Override
    protected Notifier failingNotifier() {
        return new FakeNotifier();
    }
}
