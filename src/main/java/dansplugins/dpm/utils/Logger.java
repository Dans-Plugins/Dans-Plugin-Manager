package dansplugins.dpm.utils;

import dansplugins.dpm.DansPluginManager;
import dansplugins.dpm.install.ports.DebugLog;

public class Logger implements DebugLog {
    private final DansPluginManager dansPluginManager;

    public Logger(DansPluginManager dansPluginManager) {
        this.dansPluginManager = dansPluginManager;
    }

    @Override
    public void log(String message) {
        if (dansPluginManager.isDebugEnabled()) {
            System.out.println("[DPM] " + message);
        }
    }

    @Override
    public void warn(String message) {
        System.out.println("[DPM] " + message);
    }

}
