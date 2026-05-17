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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GitHubReleaseService {
    private static final String API_URL = "https://api.github.com/repos/%s/%s/releases/latest";

    private final Logger logger;
    private String apiToken = "";
    private final ConcurrentHashMap<String, ReleaseInfo> releaseCache = new ConcurrentHashMap<>();
    private final AtomicInteger cacheGeneration = new AtomicInteger(0);

    public GitHubReleaseService(Logger logger) {
        this.logger = logger;
    }

    public void setApiToken(String token) {
        this.apiToken = token != null ? token : "";
    }

    public void clearCache() {
        cacheGeneration.incrementAndGet();
        releaseCache.clear();
    }

    String getApiToken() {
        return apiToken;
    }

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

    public ReleaseInfo getLatestReleaseMetadata(String owner, String repo) {
        return fetchRelease(owner, repo);
    }

    // generation check prevents a pre-clearCache() fetch from re-populating the cache with stale data
    private ReleaseInfo fetchRelease(String owner, String repo) {
        String key = owner + "/" + repo;
        ReleaseInfo cached = releaseCache.get(key);
        if (cached != null) return cached;

        int generation = cacheGeneration.get();
        ReleaseInfo fetched = doFetch(owner, repo);
        if (fetched != null && cacheGeneration.get() == generation) {
            releaseCache.putIfAbsent(key, fetched);
        }
        return fetched;
    }

    // package-private so tests can override via anonymous subclass without hitting the network
    ReleaseInfo doFetch(String owner, String repo) {
        String apiUrl = String.format(API_URL, owner, repo);
        HttpURLConnection connection = null;
        try {
            connection = openConnection(apiUrl);
            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                return ReleaseInfo.NO_RELEASE;
            }
            if (responseCode != 200) {
                String errorBody = readStream(connection.getErrorStream());
                logger.log("GitHub API returned HTTP " + responseCode + " for " + owner + "/" + repo + ": " + errorBody);
                return null;
            }
            String json = readStream(connection.getInputStream());
            return new ReleaseInfo(parseTagName(json), parseJarUrlFromAssets(json), parsePublishedAt(json));
        } catch (IOException e) {
            logger.log("Failed to reach GitHub API for " + owner + "/" + repo + ": " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // direct quote-scan is safe for these fields — values are simple strings with no backslash escapes
    String parseTagName(String json)     { return parseStringField(json, "tag_name"); }
    String parsePublishedAt(String json) { return parseStringField(json, "published_at"); }

    private String parseStringField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
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

    // bracket-depth tracking bounds the search to the assets array; backslash-escape handling for URL strings
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
