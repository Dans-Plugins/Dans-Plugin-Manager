package dansplugins.dpm.utils;

import dansplugins.dpm.DansPluginManager;

public class Logger {
    private final DansPluginManager dansPluginManager;

    public Logger(DansPluginManager dansPluginManager) {
        this.dansPluginManager = dansPluginManager;
    }

    public void log(String message) {
        if (dansPluginManager.isDebugEnabled()) {
            System.out.println("[DPM] " + message);
        }
    }

    public void warn(String message) {
        System.out.println("[DPM] " + message);
    }

}
