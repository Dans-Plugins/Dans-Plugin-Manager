package dansplugins.dpm.install.fakes;

import dansplugins.dpm.install.ports.Notifier;

import java.util.ArrayList;
import java.util.List;

public class FakeNotifier implements Notifier {
    public final List<String> sent = new ArrayList<>();

    @Override
    public void send(String message) {
        sent.add(message);
    }
}
