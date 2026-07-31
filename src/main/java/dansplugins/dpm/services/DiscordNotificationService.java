package dansplugins.dpm.services;

import dansplugins.dpm.repositories.ConfigRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordNotificationService {
    private final ConfigRepository configRepository;

    public DiscordNotificationService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public void send(String message) {
        String webhookUrl = configRepository.getString("discordWebhook");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        try {
            doPost(webhookUrl, message);
        } catch (Exception ignored) {}
    }

    // package-private so tests can override via anonymous subclass
    void doPost(String webhookUrl, String message) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        byte[] body = ("{\"content\":" + jsonString(message) + "}").getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(body);
        }
        connection.getResponseCode();
        connection.disconnect();
    }

    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
