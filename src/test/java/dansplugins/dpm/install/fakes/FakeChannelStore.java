package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.ChannelStore;
import dansplugins.dpm.objects.ReleaseChannel;

import java.util.HashMap;
import java.util.Map;

public class FakeChannelStore implements ChannelStore {
    public final Map<String, ReleaseChannel> pins = new HashMap<>();

    @Override
    public ReleaseChannel getChannel(String pluginName) {
        return pins.getOrDefault(pluginName.toLowerCase(), ReleaseChannel.STABLE);
    }

    @Override
    public void setChannel(String pluginName, ReleaseChannel channel) {
        pins.put(pluginName.toLowerCase(), channel);
    }

    @Override
    public void removeChannel(String pluginName) {
        pins.remove(pluginName.toLowerCase());
    }
}
