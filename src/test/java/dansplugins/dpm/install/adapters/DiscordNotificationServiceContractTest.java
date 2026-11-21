package dansplugins.dpm.install.adapters;

import dansplugins.dpm.install.ports.Notifier;
import dansplugins.dpm.install.ports.NotifierContract;
import dansplugins.dpm.repositories.ConfigRepository;

import java.io.IOException;

class DiscordNotificationServiceContractTest extends NotifierContract {
    @Override
    protected Notifier failingNotifier() {
        ConfigRepository config = new ConfigRepository(null, null, null) {
            @Override public String getString(String option) {
                return "discordWebhook".equals(option) ? "https://discord.invalid/api/webhooks/1/x" : null;
            }
        };
        return new DiscordNotificationService(config) {
            @Override
            void doPost(String webhookUrl, String message) throws IOException {
                throw new IOException("network failure");
            }
        };
    }
}
