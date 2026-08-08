package dansplugins.dpm.repositories;

import dansplugins.dpm.objects.ReleaseChannel;
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

public class GitHubReleaseRepository {
    private static final String API_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final String TAGGED_RELEASE_API_URL = "https://api.github.com/repos/%s/%s/releases/tags/%s";

    /** The rolling prerelease tag experimental builds are published under, unless config overrides it. */
    public static final String DEFAULT_EXPERIMENTAL_TAG = "dev";

    private final Logger logger;
    private String apiToken = "";
    private String experimentalTag = DEFAULT_EXPERIMENTAL_TAG;
    private final ConcurrentHashMap<String, ReleaseInfo> releaseCache = new ConcurrentHashMap<>();
    private final AtomicInteger cacheGeneration = new AtomicInteger(0);

    public GitHubReleaseRepository(Logger logger) {
        this.logger = logger;
    }

    public void setApiToken(String token) {
        this.apiToken = token != null ? token : "";
    }

    /** Sets the tag experimental builds are fetched from; blank or null restores the default. */
    public void setExperimentalTag(String tag) {
        this.experimentalTag = (tag != null && !tag.trim().isEmpty()) ? tag.trim() : DEFAULT_EXPERIMENTAL_TAG;
    }

    public String getExperimentalTag() {
        return experimentalTag;
    }

    public void clearCache() {
        cacheGeneration.incrementAndGet();
        releaseCache.clear();
    }

    String getApiToken() {
        return apiToken;
    }

    public ReleaseInfo getLatestRelease(String owner, String repo) {
        return withJarAsset(fetchRelease(owner, repo, ReleaseChannel.STABLE), owner, repo);
    }

    /**
     * Fetches the rolling main-branch prerelease. Returns {@link ReleaseInfo#NO_RELEASE} when the
     * repository publishes no experimental build (the tag 404s), matching the stable path.
     */
    public ReleaseInfo getExperimentalRelease(String owner, String repo) {
        return withJarAsset(fetchRelease(owner, repo, ReleaseChannel.EXPERIMENTAL), owner, repo);
    }

    public ReleaseInfo getRelease(String owner, String repo, ReleaseChannel channel) {
        return channel == ReleaseChannel.EXPERIMENTAL
                ? getExperimentalRelease(owner, repo)
                : getLatestRelease(owner, repo);
    }

    private ReleaseInfo withJarAsset(ReleaseInfo release, String owner, String repo) {
        if (release == null || release == ReleaseInfo.NO_RELEASE) return release;
        if (release.getJarUrl() == null) {
            logger.warn("Release " + release.getTagName() + " for " + owner + "/" + repo + " has no .jar asset.");
            return null;
        }
        return release;
    }

    HttpURLConnection openConnection(String apiUrl) throws IOException {
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
        return fetchRelease(owner, repo, ReleaseChannel.STABLE);
    }

    public ReleaseInfo getReleaseMetadata(String owner, String repo, ReleaseChannel channel) {
        return fetchRelease(owner, repo, channel);
    }

    // generation check prevents a pre-clearCache() fetch from re-populating the cache with stale data
    private ReleaseInfo fetchRelease(String owner, String repo, ReleaseChannel channel) {
        String key = cacheKey(owner, repo, channel);
        ReleaseInfo cached = releaseCache.get(key);
        if (cached != null) return cached;

        int generation = cacheGeneration.get();
        ReleaseInfo fetched = fetchForChannel(owner, repo, channel);
        if (fetched != null && cacheGeneration.get() == generation) {
            releaseCache.putIfAbsent(key, fetched);
        }
        return fetched;
    }

    // The two channels return different releases for the same repo, so they must not share a key.
    String cacheKey(String owner, String repo, ReleaseChannel channel) {
        String base = owner + "/" + repo;
        return channel == ReleaseChannel.EXPERIMENTAL ? base + "@" + experimentalTag : base;
    }

    private ReleaseInfo fetchForChannel(String owner, String repo, ReleaseChannel channel) {
        return channel == ReleaseChannel.EXPERIMENTAL ? doFetchExperimental(owner, repo) : doFetch(owner, repo);
    }

    // package-private so tests can override via anonymous subclass without hitting the network
    ReleaseInfo doFetch(String owner, String repo) {
        return fetchFrom(String.format(API_URL, owner, repo), owner, repo, ReleaseChannel.STABLE);
    }

    // package-private for the same reason as doFetch()
    ReleaseInfo doFetchExperimental(String owner, String repo) {
        String apiUrl = String.format(TAGGED_RELEASE_API_URL, owner, repo, experimentalTag);
        return fetchFrom(apiUrl, owner, repo, ReleaseChannel.EXPERIMENTAL);
    }

    private ReleaseInfo fetchFrom(String apiUrl, String owner, String repo, ReleaseChannel channel) {
        for (int attempt = 0; attempt < 2; attempt++) {
            if (attempt > 0) sleepMs(2000);
            HttpURLConnection connection = null;
            try {
                connection = openConnection(apiUrl);
                int responseCode = connection.getResponseCode();
                if (responseCode == 404) {
                    return ReleaseInfo.NO_RELEASE;
                }
                if (responseCode == 401) {
                    logger.warn("GitHub API rejected the configured token for " + owner + "/" + repo
                            + " — verify or clear githubToken in config.yml.");
                    return null;
                }
                if (responseCode == 429
                        || (responseCode == 403 && "0".equals(connection.getHeaderField("X-RateLimit-Remaining")))) {
                    logger.warn("GitHub rate limit reached for " + owner + "/" + repo
                            + " — configure a githubToken in config.yml to raise the limit.");
                    return null;
                }
                if (responseCode >= 500) {
                    if (attempt == 0) continue;
                    String errorBody = readStream(connection.getErrorStream());
                    logger.warn("GitHub API returned HTTP " + responseCode + " for " + owner + "/" + repo + ": " + errorBody);
                    return null;
                }
                if (responseCode != 200) {
                    String errorBody = readStream(connection.getErrorStream());
                    logger.warn("GitHub API returned HTTP " + responseCode + " for " + owner + "/" + repo + ": " + errorBody);
                    return null;
                }
                String json = readStream(connection.getInputStream());
                String publishedAt = parsePublishedAt(json);
                String version = channel == ReleaseChannel.EXPERIMENTAL
                        ? synthesizeExperimentalVersion(parseTagName(json), parseTargetCommitish(json), publishedAt)
                        : parseTagName(json);
                return new ReleaseInfo(version, parseJarUrlFromAssets(json), publishedAt);
            } catch (IOException e) {
                if (attempt == 0) continue;
                logger.warn("Failed to reach GitHub API for " + owner + "/" + repo + ": " + e.getMessage());
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

    void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // direct quote-scan is safe for these fields — values are simple strings with no backslash escapes
    String parseTagName(String json)         { return parseStringField(json, "tag_name"); }
    String parsePublishedAt(String json)     { return parseStringField(json, "published_at"); }
    String parseTargetCommitish(String json) { return parseStringField(json, "target_commitish"); }

    /**
     * Builds a version identity for an experimental release.
     *
     * <p>Every experimental build carries the same tag, so the tag alone can never distinguish
     * yesterday's build from today's — a plugin pinned to it would report "already up to date"
     * forever. The commit the release points at is appended to make each build distinct, with the
     * publish timestamp as a fallback for repositories whose {@code target_commitish} is a branch
     * name rather than a sha.
     */
    String synthesizeExperimentalVersion(String tagName, String targetCommitish, String publishedAt) {
        String base = (tagName != null && !tagName.isEmpty()) ? tagName : experimentalTag;
        if (isCommitSha(targetCommitish)) {
            return base + "-" + targetCommitish.substring(0, 7);
        }
        if (publishedAt != null && !publishedAt.isEmpty()) {
            return base + "@" + publishedAt;
        }
        return base;
    }

    private boolean isCommitSha(String value) {
        if (value == null || value.length() != 40) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) return false;
        }
        return true;
    }

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
