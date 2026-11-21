package dansplugins.dpm.install.ports;

/**
 * DPM's own diagnostic output. {@link #log} lines appear only when debug mode is on;
 * {@link #warn} lines always appear. Never throws. Safe from any thread.
 */
public interface DebugLog {
    void log(String message);

    void warn(String message);
}
