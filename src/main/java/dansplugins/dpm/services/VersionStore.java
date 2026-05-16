package dansplugins.dpm.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persists the last-downloaded release tag for each managed plugin so that
 * re-downloads can be skipped when the plugin is already on the latest version.
 *
 * Tags are keyed by lower-cased plugin name and stored in a .properties file
 * inside the plugin's data folder.
 */
public class VersionStore {
    private final File storeFile;
    private final Properties props = new Properties();

    public VersionStore(File storeFile) {
        this.storeFile = storeFile;
        load();
    }

    /** Returns the stored release tag for the given plugin, or null if unknown. */
    public String getStoredTag(String pluginName) {
        return props.getProperty(pluginName.toLowerCase());
    }

    /** Persists the release tag for the given plugin. */
    public void setTag(String pluginName, String tag) {
        props.setProperty(pluginName.toLowerCase(), tag);
        save();
    }

    /** Removes the stored tag for the given plugin. */
    public void removeTag(String pluginName) {
        props.remove(pluginName.toLowerCase());
        save();
    }

    private void load() {
        if (!storeFile.exists()) return;
        try (FileInputStream in = new FileInputStream(storeFile)) {
            props.load(in);
        } catch (IOException ignored) {
        }
    }

    private void save() {
        storeFile.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(storeFile)) {
            props.store(out, null);
        } catch (IOException ignored) {
        }
    }
}
