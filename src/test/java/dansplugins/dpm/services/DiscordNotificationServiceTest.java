package dansplugins.dpm.services;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiscordNotificationServiceTest {

    // -------------------------------------------------------------------------
    // send()
    // -------------------------------------------------------------------------

    @Test
    void send_doesNothingWhenWebhookUrlIsEmpty() {
        List<String> posted = new ArrayList<>();
        DiscordNotificationService svc = new DiscordNotificationService(stubConfig("")) {
            @Override void doPost(String url, String message) {
                posted.add(url);
            }
        };
        svc.send("hello");
        assertTrue(posted.isEmpty(), "doPost must not be called when webhook URL is empty");
    }

    @Test
    void send_doesNothingWhenWebhookUrlIsNull() {
        List<String> posted = new ArrayList<>();
        DiscordNotificationService svc = new DiscordNotificationService(stubConfig(null)) {
            @Override void doPost(String url, String message) {
                posted.add(url);
            }
        };
        svc.send("hello");
        assertTrue(posted.isEmpty(), "doPost must not be called when webhook URL is null");
    }

    @Test
    void send_callsDoPostWithCorrectArguments() throws IOException {
        List<String[]> calls = new ArrayList<>();
        DiscordNotificationService svc = new DiscordNotificationService(stubConfig("https://discord.com/api/webhooks/123/abc")) {
            @Override void doPost(String url, String message) {
                calls.add(new String[]{url, message});
            }
        };
        svc.send("test message");
        assertEquals(1, calls.size(), "doPost must be called once");
        assertEquals("https://discord.com/api/webhooks/123/abc", calls.get(0)[0]);
        assertEquals("test message", calls.get(0)[1]);
    }

    @Test
    void send_silentlyIgnoresDoPostFailure() {
        DiscordNotificationService svc = new DiscordNotificationService(stubConfig("https://discord.com/api/webhooks/123/abc")) {
            @Override void doPost(String url, String message) throws IOException {
                throw new IOException("network failure");
            }
        };
        assertDoesNotThrow(() -> svc.send("test message"),
                "Exceptions from doPost must not propagate out of send()");
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private ConfigService stubConfig(String webhookUrl) {
        return new ConfigService(null) {
            @Override public String getString(String option) {
                return "discordWebhook".equals(option) ? webhookUrl : null;
            }
        };
    }
}
