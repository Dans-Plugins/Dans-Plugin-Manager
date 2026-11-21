package dansplugins.dpm.install.ports;

import dansplugins.dpm.objects.ReleaseChannel;

/**
 * Remembers the release channel each plugin is pinned to. Names are matched ignoring case.
 *
 * <p>A plugin with no entry is on {@link ReleaseChannel#STABLE}. Failed writes are reported and
 * swallowed, never thrown.
 */
public interface ChannelStore {
    ReleaseChannel getChannel(String pluginName);

    void setChannel(String pluginName, ReleaseChannel channel);

    /** Drops the pin, returning the plugin to STABLE. */
    void removeChannel(String pluginName);
}
