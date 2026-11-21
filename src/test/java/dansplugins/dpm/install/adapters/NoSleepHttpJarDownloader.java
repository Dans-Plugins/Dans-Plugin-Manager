package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.DebugLog;

/** Test helper: an HttpJarDownloader whose retry sleep is a no-op, usable from other test packages. */
public class NoSleepHttpJarDownloader extends HttpJarDownloader {
    public NoSleepHttpJarDownloader(DebugLog logger) {
        super(logger);
    }

    @Override
    void sleepMs(long ms) {}
}
