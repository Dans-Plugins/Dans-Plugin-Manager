package dansplugins.dpm.install.ports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Contract every Notifier must meet: best effort, never throws. */
public abstract class NotifierContract {

    /** A notifier whose destination fails on every send. */
    protected abstract Notifier failingNotifier();

    // -------------------------------------------------------------------------
    // send()
    // -------------------------------------------------------------------------

    @Test
    void send_neverThrowsEvenWhenTheDestinationFails() {
        assertDoesNotThrow(() -> failingNotifier().send("[DPM] \"quoted\"\nsecond line"));
    }
}
