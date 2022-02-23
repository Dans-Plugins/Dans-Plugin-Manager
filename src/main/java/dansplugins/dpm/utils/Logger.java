package dansplugins.dpm.utils;

import dansplugins.dpm.DansPluginManager;

/**
 * @author Daniel McCoy Stephenson
 */
public class Logger {

    private static Logger instance;

    private Logger() {

    }

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void log(String message) {
        if (DansPluginManager.getInstance().isDebugEnabled()) {
            System.out.println("[DPM] " + message);
        }
    }

}
