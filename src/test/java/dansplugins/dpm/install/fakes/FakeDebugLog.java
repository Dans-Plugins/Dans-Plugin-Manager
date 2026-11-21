package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.DebugLog;

import java.util.ArrayList;
import java.util.List;

public class FakeDebugLog implements DebugLog {
    public final List<String> debug = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();

    @Override
    public void log(String message) {
        debug.add(message);
    }

    @Override
    public void warn(String message) {
        warnings.add(message);
    }
}
