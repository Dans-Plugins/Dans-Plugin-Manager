package dansplugins.dpm.install.ports;

/**
 * Remembers which release tag is installed for each plugin. Names are matched ignoring case.
 *
 * <p>A failed write is reported to the operator and swallowed, never thrown; the value still
 * holds for the rest of the session.
 */
public interface VersionStore {
    /** The stored tag, or null if none is known. */
    String getStoredTag(String pluginName);

    void setTag(String pluginName, String tag);
}
