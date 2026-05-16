package dansplugins.dpm.services;

import dansplugins.dpm.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GitHubReleaseService {
    private static final String API_URL = "https://api.github.com/repos/%s/%s/releases/latest";

    /** Returned when the repo exists but has no published (non-prerelease) release yet. */
    public static final String NO_RELEASE = "__NO_RELEASE__";

    private final Logger logger;

    public GitHubReleaseService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns the browser_download_url of the first .jar asset on the latest release.
     * Returns {@link #NO_RELEASE} when GitHub reports 404 (no releases published yet).
     * Returns null on network or other errors.
     */
    public String getLatestJarDownloadUrl(String owner, String repo) {
        String apiUrl = String.format(API_URL, owner, repo);
        HttpURLConnection connection = null;
        try {
            connection = openConnection(apiUrl);
            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                return NO_RELEASE;
            }
            if (responseCode != 200) {
                String errorBody = readStream(connection.getErrorStream());
                logger.log("GitHub API returned HTTP " + responseCode + " for " + owner + "/" + repo + ": " + errorBody);
                return null;
            }
            String json = readStream(connection.getInputStream());
            return parseJarUrlFromAssets(json);
        } catch (IOException e) {
            logger.log("Failed to reach GitHub API for " + owner + "/" + repo + ": " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(String apiUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "Dans-Plugin-Manager");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Extracts the first .jar browser_download_url from within the assets array.
     * Uses bracket-depth tracking to bound the search strictly to the assets array,
     * and handles backslash-escaped characters within URL strings.
     */
    private String parseJarUrlFromAssets(String json) {
        int assetsKeyIndex = json.indexOf("\"assets\":");
        if (assetsKeyIndex == -1) return null;

        int arrayOpen = json.indexOf('[', assetsKeyIndex);
        if (arrayOpen == -1) return null;

        // Find the matching closing bracket via depth tracking
        int depth = 0;
        int arrayClose = -1;
        for (int i = arrayOpen; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    arrayClose = i;
                    break;
                }
            }
        }
        if (arrayClose == -1) return null;

        String assetsJson = json.substring(arrayOpen, arrayClose + 1);
        String key = "\"browser_download_url\"";
        int searchFrom = 0;
        while (true) {
            int keyIndex = assetsJson.indexOf(key, searchFrom);
            if (keyIndex == -1) break;
            int colonIndex = assetsJson.indexOf(':', keyIndex + key.length());
            if (colonIndex == -1) break;
            int openQuote = assetsJson.indexOf('"', colonIndex + 1);
            if (openQuote == -1) break;
            // Walk characters, respecting backslash escapes
            StringBuilder url = new StringBuilder();
            int i = openQuote + 1;
            while (i < assetsJson.length()) {
                char c = assetsJson.charAt(i);
                if (c == '\\') {
                    i += 2;
                    continue;
                }
                if (c == '"') break;
                url.append(c);
                i++;
            }
            String downloadUrl = url.toString();
            if (downloadUrl.endsWith(".jar")) {
                return downloadUrl;
            }
            searchFrom = i + 1;
        }
        return null;
    }
}
