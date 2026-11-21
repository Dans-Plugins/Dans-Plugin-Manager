package dansplugins.dpm.install.ports;

/**
 * Tells the server's operators that something happened (an install failed, an update finished).
 *
 * <p>Best effort: never throws, and silently does nothing when no destination is configured.
 * May block on the network.
 */
public interface Notifier {
    void send(String message);
}
