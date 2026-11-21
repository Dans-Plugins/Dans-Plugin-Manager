package dansplugins.dpm.utils;

import dansplugins.dpm.install.ports.DebugLog;
import dansplugins.dpm.install.ports.DebugLogContract;

class LoggerContractTest extends DebugLogContract {
    @Override
    protected DebugLog debugLog() {
        return new Logger(null);
    }
}
