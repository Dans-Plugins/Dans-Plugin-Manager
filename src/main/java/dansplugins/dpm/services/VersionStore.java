package dansplugins.dpm.services;

import dansplugins.dpm.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class VersionStore {
    private final Logger logger;
    private final File storeFile;
    private final Properties props = new Properties();

    public VersionStore(File storeFile, Logger logger) {
        this.logger = logger;
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
        } catch (IOException e) {
            logger.warn("Could not load version store (" + e.getMessage()
                    + ") — all plugins will be treated as unversioned this session.");
        }
    }

    private void save() {
        storeFile.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(storeFile)) {
            props.store(out, null);
        } catch (IOException e) {
            logger.warn("Could not save version store (" + e.getMessage()
                    + ") — plugin version data will not persist across restarts.");
        }
    }
}
