package dansplugins.dpm.services;

import dansplugins.dpm.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubReleaseService {
    private static final String API_URL = "https://api.github.com/repos/%s/%s/releases/latest";

    private final Logger logger;

    public GitHubReleaseService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns the browser_download_url of the first .jar asset on the latest release,
     * or null if none could be found.
     */
    public String getLatestJarDownloadUrl(String owner, String repo) {
        String apiUrl = String.format(API_URL, owner, repo);
        try {
            String json = fetchJson(apiUrl);
            return parseJarUrl(json);
        } catch (IOException e) {
            logger.log("Failed to fetch release info for " + owner + "/" + repo + ": " + e.getMessage());
            return null;
        }
    }

    private String fetchJson(String apiUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Minimal JSON extraction — finds the first "browser_download_url" whose value ends in ".jar".
     * Avoids pulling in a JSON library so the plugin stays lightweight.
     */
    private String parseJarUrl(String json) {
        String key = "\"browser_download_url\"";
        int searchFrom = 0;
        while (true) {
            int keyIndex = json.indexOf(key, searchFrom);
            if (keyIndex == -1) break;
            int colonIndex = json.indexOf(':', keyIndex + key.length());
            if (colonIndex == -1) break;
            int openQuote = json.indexOf('"', colonIndex + 1);
            if (openQuote == -1) break;
            int closeQuote = json.indexOf('"', openQuote + 1);
            if (closeQuote == -1) break;
            String url = json.substring(openQuote + 1, closeQuote);
            if (url.endsWith(".jar")) {
                return url;
            }
            searchFrom = closeQuote + 1;
        }
        return null;
    }
}
