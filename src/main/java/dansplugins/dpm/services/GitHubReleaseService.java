package dansplugins.dpm.services;

import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GitHubReleaseService {
    private static final String API_URL = "https://api.github.com/repos/%s/%s/releases/latest";

    private final Logger logger;
    private String apiToken = "";
    private final Map<String, ReleaseInfo> releaseCache = new HashMap<>();

    public GitHubReleaseService(Logger logger) {
        this.logger = logger;
    }

    public void setApiToken(String token) {
        this.apiToken = token != null ? token : "";
    }

    public void clearCache() {
        releaseCache.clear();
    }

    String getApiToken() {
        return apiToken;
    }

    /**
     * Returns a {@link ReleaseInfo} with the tag name and first .jar asset URL for the
     * latest release. Returns {@link ReleaseInfo#NO_RELEASE} when GitHub reports 404
     * (no releases published yet). Returns null when no .jar asset exists or on network errors.
     * Results are cached for the session; call {@link #clearCache()} to force a fresh fetch.
     */
    public ReleaseInfo getLatestRelease(String owner, String repo) {
        ReleaseInfo release = fetchRelease(owner, repo);
        if (release == null || release == ReleaseInfo.NO_RELEASE) return release;
        if (release.getJarUrl() == null) {
            logger.log("Release " + release.getTagName() + " for " + owner + "/" + repo + " has no .jar asset.");
            return null;
        }
        return release;
    }

    private HttpURLConnection openConnection(String apiUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "Dans-Plugin-Manager");
        if (!apiToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + apiToken);
        }
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
     * Returns a {@link ReleaseInfo} with tag name and publish date for the latest release,
     * without requiring a downloadable .jar asset. Returns {@link ReleaseInfo#NO_RELEASE}
     * on 404, or null on network/other errors.
     * Results are cached for the session; call {@link #clearCache()} to force a fresh fetch.
     */
    public ReleaseInfo getLatestReleaseMetadata(String owner, String repo) {
        return fetchRelease(owner, repo);
    }

    /**
     * Fetches and caches the full release info (tag, jar URL, publish date) for the given repo.
     * On 404 caches and returns {@link ReleaseInfo#NO_RELEASE}. Network errors return null and
     * are not cached so the next call can retry.
     */
    private ReleaseInfo fetchRelease(String owner, String repo) {
        String key = owner + "/" + repo;
        ReleaseInfo cached = releaseCache.get(key);
        if (cached != null) return cached;

        String apiUrl = String.format(API_URL, owner, repo);
        HttpURLConnection connection = null;
        try {
            connection = openConnection(apiUrl);
            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                releaseCache.put(key, ReleaseInfo.NO_RELEASE);
                return ReleaseInfo.NO_RELEASE;
            }
            if (responseCode != 200) {
                String errorBody = readStream(connection.getErrorStream());
                logger.log("GitHub API returned HTTP " + responseCode + " for " + owner + "/" + repo + ": " + errorBody);
                return null;
            }
            String json = readStream(connection.getInputStream());
            ReleaseInfo release = new ReleaseInfo(parseTagName(json), parseJarUrlFromAssets(json), parsePublishedAt(json));
            releaseCache.put(key, release);
            return release;
        } catch (IOException e) {
            logger.log("Failed to reach GitHub API for " + owner + "/" + repo + ": " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Extracts the value of the top-level "tag_name" field. Tag names are simple
     * strings that never contain backslash escapes, so a direct quote scan suffices.
     */
    String parseTagName(String json) {
        String key = "\"tag_name\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(':', keyIndex + key.length());
        if (colonIndex == -1) return null;
        int openQuote = json.indexOf('"', colonIndex + 1);
        if (openQuote == -1) return null;
        int closeQuote = json.indexOf('"', openQuote + 1);
        if (closeQuote == -1) return null;
        return json.substring(openQuote + 1, closeQuote);
    }

    /**
     * Extracts the value of the top-level "published_at" field (ISO 8601 date string).
     * Uses the same quote-scan approach as parseTagName.
     */
    String parsePublishedAt(String json) {
        String key = "\"published_at\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(':', keyIndex + key.length());
        if (colonIndex == -1) return null;
        int openQuote = json.indexOf('"', colonIndex + 1);
        if (openQuote == -1) return null;
        int closeQuote = json.indexOf('"', openQuote + 1);
        if (closeQuote == -1) return null;
        return json.substring(openQuote + 1, closeQuote);
    }

    /**
     * Extracts the first .jar browser_download_url from within the assets array.
     * Uses bracket-depth tracking to bound the search strictly to the assets array,
     * and handles backslash-escaped characters within URL strings.
     */
    String parseJarUrlFromAssets(String json) {
        int assetsKeyIndex = json.indexOf("\"assets\":");
        if (assetsKeyIndex == -1) return null;

        int arrayOpen = json.indexOf('[', assetsKeyIndex);
        if (arrayOpen == -1) return null;

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
