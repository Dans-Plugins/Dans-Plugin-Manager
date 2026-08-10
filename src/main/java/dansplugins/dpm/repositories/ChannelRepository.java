package dansplugins.dpm.repositories;

import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persists the release channel each plugin is pinned to.
 *
 * <p>Kept in its own properties file rather than as prefixed keys in the version store so that
 * the version store's format stays unchanged. Plugins with no stored channel are treated as
 * {@link ReleaseChannel#STABLE}, which is what every plugin installed before this feature is.
 */
public class ChannelRepository {
    private final Logger logger;
    private final File storeFile;
    private final Properties props = new Properties();

    public ChannelRepository(File storeFile, Logger logger) {
        this.logger = logger;
        this.storeFile = storeFile;
        load();
    }

    /** Returns the channel the given plugin is pinned to, defaulting to STABLE. */
    public ReleaseChannel getChannel(String pluginName) {
        String stored = props.getProperty(pluginName.toLowerCase());
        ReleaseChannel channel = ReleaseChannel.fromStored(stored);
        // A hand-edited or corrupted entry silently demotes a plugin to stable, which looks like the
        // pin was forgotten rather than rejected — so say which value was not understood.
        if (stored != null && !stored.trim().isEmpty() && !stored.trim().equalsIgnoreCase(channel.name())) {
            warn("Unrecognised channel '" + stored + "' stored for " + pluginName
                    + " — treating it as " + channel.getDisplayName() + ".");
        }
        return channel;
    }

    /** Pins the given plugin to a channel. */
    public void setChannel(String pluginName, ReleaseChannel channel) {
        props.setProperty(pluginName.toLowerCase(), channel.getDisplayName());
        save();
    }

    /** Removes the stored channel for the given plugin, returning it to the STABLE default. */
    public void removeChannel(String pluginName) {
        props.remove(pluginName.toLowerCase());
        save();
    }

    private void load() {
        if (!storeFile.exists()) return;
        try (FileInputStream in = new FileInputStream(storeFile)) {
            props.load(in);
        } catch (IOException e) {
            warn("Could not load channel store (" + e.getMessage()
                    + ") — all plugins will be treated as stable this session.");
        }
    }

    private void save() {
        storeFile.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(storeFile)) {
            props.store(out, null);
        } catch (IOException e) {
            warn("Could not save channel store (" + e.getMessage()
                    + ") — plugin channel data will not persist across restarts.");
        }
    }

    private void warn(String message) {
        if (logger != null) logger.warn(message);
    }
}
