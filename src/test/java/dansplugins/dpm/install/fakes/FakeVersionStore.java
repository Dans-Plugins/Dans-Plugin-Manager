package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.VersionStore;

import java.util.HashMap;
import java.util.Map;

public class FakeVersionStore implements VersionStore {
    public final Map<String, String> tags = new HashMap<>();

    @Override
    public String getStoredTag(String pluginName) {
        return tags.get(pluginName.toLowerCase());
    }

    @Override
    public void setTag(String pluginName, String tag) {
        tags.put(pluginName.toLowerCase(), tag);
    }
}
