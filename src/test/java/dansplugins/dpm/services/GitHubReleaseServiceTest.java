package dansplugins.dpm.services;

import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.utils.Logger;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GitHubReleaseServiceTest {

    // Logger is only used for HTTP errors; null is safe for pure parsing tests.
    private final GitHubReleaseService service = new GitHubReleaseService(null);

    // -------------------------------------------------------------------------
    // parseJarUrlFromAssets()
    // -------------------------------------------------------------------------

    @Test
    void parseJarUrlFromAssets_returnsJarUrl() {
        String json = "{\"assets\":[{\"name\":\"Plugin-1.0.jar\"," +
                "\"browser_download_url\":\"https://github.com/org/repo/releases/download/v1.0/Plugin-1.0.jar\"}]}";
        assertEquals(
                "https://github.com/org/repo/releases/download/v1.0/Plugin-1.0.jar",
                service.parseJarUrlFromAssets(json)
        );
    }

    @Test
    void parseJarUrlFromAssets_skipsNonJarAssets() {
        String json = "{\"assets\":[" +
                "{\"name\":\"checksums.txt\",\"browser_download_url\":\"https://example.com/checksums.txt\"}," +
                "{\"name\":\"Plugin-1.0.jar\",\"browser_download_url\":\"https://example.com/Plugin-1.0.jar\"}" +
                "]}";
        assertEquals("https://example.com/Plugin-1.0.jar", service.parseJarUrlFromAssets(json));
    }

    @Test
    void parseJarUrlFromAssets_returnsNullWhenNoJarAsset() {
        String json = "{\"assets\":[{\"name\":\"checksums.txt\"," +
                "\"browser_download_url\":\"https://example.com/checksums.txt\"}]}";
        assertNull(service.parseJarUrlFromAssets(json));
    }

    @Test
    void parseJarUrlFromAssets_returnsNullForEmptyAssetsArray() {
        assertNull(service.parseJarUrlFromAssets("{\"assets\":[]}"));
    }

    @Test
    void parseJarUrlFromAssets_returnsNullWhenNoAssetsKey() {
        assertNull(service.parseJarUrlFromAssets("{\"tag_name\":\"v1.0\"}"));
    }

    @Test
    void parseJarUrlFromAssets_ignoresFakeUrlInReleaseBody() {
        // A release body containing the literal text "browser_download_url" must not
        // be matched — the search must be scoped to the assets array only.
        String json = "{\"body\":\"see \\\"browser_download_url\\\": \\\"https://evil.com/fake.jar\\\" for details\"," +
                "\"assets\":[{\"name\":\"Real-1.0.jar\"," +
                "\"browser_download_url\":\"https://github.com/org/repo/releases/download/v1.0/Real-1.0.jar\"}]}";
        assertEquals(
                "https://github.com/org/repo/releases/download/v1.0/Real-1.0.jar",
                service.parseJarUrlFromAssets(json)
        );
    }

    @Test
    void parseJarUrlFromAssets_handlesEscapedCharactersInUrl() {
        // Ensure backslash-escaped sequences in the URL string don't break extraction.
        String json = "{\"assets\":[{\"name\":\"Plugin-1.0.jar\"," +
                "\"browser_download_url\":\"https://example.com/path\\/Plugin-1.0.jar\"}]}";
        assertTrue(service.parseJarUrlFromAssets(json).endsWith(".jar"));
    }

    @Test
    void parseJarUrlFromAssets_doesNotMatchBeyondAssetsArray() {
        // Fields after the closing ] of assets must not be scanned.
        String json = "{\"assets\":[]," +
                "\"uploader\":{\"browser_download_url\":\"https://example.com/outside.jar\"}}";
        assertNull(service.parseJarUrlFromAssets(json));
    }

    @Test
    void parseJarUrlFromAssets_handlesMultipleAssets() {
        String json = "{\"assets\":[" +
                "{\"name\":\"Plugin-1.0-sources.jar\",\"browser_download_url\":\"https://example.com/Plugin-1.0-sources.jar\"}," +
                "{\"name\":\"Plugin-1.0.jar\",\"browser_download_url\":\"https://example.com/Plugin-1.0.jar\"}" +
                "]}";
        // Should return the first .jar match
        assertEquals("https://example.com/Plugin-1.0-sources.jar", service.parseJarUrlFromAssets(json));
    }

    // -------------------------------------------------------------------------
    // parseTagName()
    // -------------------------------------------------------------------------

    @Test
    void parseTagName_returnsTag() {
        String json = "{\"tag_name\":\"v4.6.3\",\"assets\":[]}";
        assertEquals("v4.6.3", service.parseTagName(json));
    }

    @Test
    void parseTagName_returnsNullWhenMissing() {
        assertNull(service.parseTagName("{\"assets\":[]}"));
    }

    @Test
    void parseTagName_returnsNullForEmptyString() {
        assertNull(service.parseTagName(""));
    }

    @Test
    void parseTagName_handlesTagWithoutVPrefix() {
        String json = "{\"tag_name\":\"4.6.3\",\"assets\":[]}";
        assertEquals("4.6.3", service.parseTagName(json));
    }

    // -------------------------------------------------------------------------
    // parsePublishedAt()
    // -------------------------------------------------------------------------

    @Test
    void parsePublishedAt_returnsDate() {
        String json = "{\"tag_name\":\"v1.0\",\"published_at\":\"2024-03-15T10:00:00Z\",\"assets\":[]}";
        assertEquals("2024-03-15T10:00:00Z", service.parsePublishedAt(json));
    }

    @Test
    void parsePublishedAt_returnsNullWhenMissing() {
        assertNull(service.parsePublishedAt("{\"tag_name\":\"v1.0\",\"assets\":[]}"));
    }

    @Test
    void parsePublishedAt_returnsNullForEmptyString() {
        assertNull(service.parsePublishedAt(""));
    }

    // -------------------------------------------------------------------------
    // cache behaviour (via doFetch override)
    // -------------------------------------------------------------------------

    @Test
    void getLatestRelease_returnsCachedResultOnSecondCall() {
        AtomicInteger fetchCount = new AtomicInteger(0);
        GitHubReleaseService svc = new GitHubReleaseService(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                fetchCount.incrementAndGet();
                return new ReleaseInfo("v1.0", "https://example.com/plugin.jar", "2024-01-01T00:00:00Z");
            }
        };
        svc.getLatestRelease("org", "repo");
        svc.getLatestRelease("org", "repo");
        assertEquals(1, fetchCount.get(), "Second call for same repo should use cache");
    }

    @Test
    void clearCache_causesRefetchOnNextCall() {
        AtomicInteger fetchCount = new AtomicInteger(0);
        GitHubReleaseService svc = new GitHubReleaseService(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                fetchCount.incrementAndGet();
                return new ReleaseInfo("v1.0", "https://example.com/plugin.jar", "2024-01-01T00:00:00Z");
            }
        };
        svc.getLatestRelease("org", "repo");
        svc.clearCache();
        svc.getLatestRelease("org", "repo");
        assertEquals(2, fetchCount.get(), "Call after clearCache should refetch");
    }

    @Test
    void differentRepos_eachFetchedOnce() {
        AtomicInteger fetchCount = new AtomicInteger(0);
        GitHubReleaseService svc = new GitHubReleaseService(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                fetchCount.incrementAndGet();
                return new ReleaseInfo("v1.0", "https://example.com/" + repo + ".jar", null);
            }
        };
        svc.getLatestRelease("org", "repo-a");
        svc.getLatestRelease("org", "repo-a");
        svc.getLatestRelease("org", "repo-b");
        svc.getLatestRelease("org", "repo-b");
        assertEquals(2, fetchCount.get(), "Each distinct repo should be fetched once");
    }

    @Test
    void noRelease_isCached() {
        AtomicInteger fetchCount = new AtomicInteger(0);
        GitHubReleaseService svc = new GitHubReleaseService(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                fetchCount.incrementAndGet();
                return ReleaseInfo.NO_RELEASE;
            }
        };
        svc.getLatestReleaseMetadata("org", "repo");
        svc.getLatestReleaseMetadata("org", "repo");
        assertEquals(1, fetchCount.get(), "NO_RELEASE should be cached to avoid repeated 404 requests");
    }

    @Test
    void networkError_isNotCached() {
        AtomicInteger fetchCount = new AtomicInteger(0);
        GitHubReleaseService svc = new GitHubReleaseService(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                fetchCount.incrementAndGet();
                return null;
            }
        };
        svc.getLatestReleaseMetadata("org", "repo");
        svc.getLatestReleaseMetadata("org", "repo");
        assertEquals(2, fetchCount.get(), "Network errors must not be cached so the next call can retry");
    }

    // -------------------------------------------------------------------------
    // setApiToken()
    // -------------------------------------------------------------------------

    @Test
    void setApiToken_nullTreatedAsEmpty() {
        service.setApiToken(null);
        assertEquals("", service.getApiToken());
    }

    @Test
    void setApiToken_emptyStringStoredAsEmpty() {
        service.setApiToken("");
        assertEquals("", service.getApiToken());
    }

    @Test
    void setApiToken_validTokenStored() {
        service.setApiToken("ghp_exampletoken123");
        assertEquals("ghp_exampletoken123", service.getApiToken());
    }

    @Test
    void setApiToken_overwritesPreviousToken() {
        service.setApiToken("first_token");
        service.setApiToken("second_token");
        assertEquals("second_token", service.getApiToken());
    }

    // -------------------------------------------------------------------------
    // doFetch() — HTTP error code distinction (#79, #88)
    // -------------------------------------------------------------------------

    @Test
    void doFetch_returnsNull_andWarns_onHttp401() throws IOException {
        List<String> warnings = new ArrayList<>();
        GitHubReleaseService svc = new GitHubReleaseService(capturingLogger(warnings)) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                return fakeConnection(401, null);
            }
        };
        assertNull(svc.doFetch("org", "repo"));
        assertTrue(warnings.stream().anyMatch(m -> m.contains("token") && m.contains("org/repo")),
                "401 must produce a token-specific warning");
    }

    @Test
    void doFetch_returnsNull_andWarns_onHttp429RateLimit() throws IOException {
        List<String> warnings = new ArrayList<>();
        GitHubReleaseService svc = new GitHubReleaseService(capturingLogger(warnings)) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                return fakeConnection(429, null);
            }
        };
        assertNull(svc.doFetch("org", "repo"));
        assertTrue(warnings.stream().anyMatch(m -> m.contains("rate limit") && m.contains("org/repo")),
                "429 must produce a rate-limit warning");
    }

    @Test
    void doFetch_returnsNull_andWarns_onHttp403WithZeroRateLimitRemaining() throws IOException {
        List<String> warnings = new ArrayList<>();
        GitHubReleaseService svc = new GitHubReleaseService(capturingLogger(warnings)) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                return fakeConnection(403, "0");
            }
        };
        assertNull(svc.doFetch("org", "repo"));
        assertTrue(warnings.stream().anyMatch(m -> m.contains("rate limit") && m.contains("org/repo")),
                "403 + X-RateLimit-Remaining: 0 must produce a rate-limit warning");
    }

    @Test
    void doFetch_returnsNull_andWarns_onGenericNon200() throws IOException {
        List<String> warnings = new ArrayList<>();
        GitHubReleaseService svc = new GitHubReleaseService(capturingLogger(warnings)) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                return fakeConnection(500, null);
            }
        };
        assertNull(svc.doFetch("org", "repo"));
        assertTrue(warnings.stream().anyMatch(m -> m.contains("500")),
                "Generic non-200 must warn with the HTTP status code");
    }

    // -------------------------------------------------------------------------
    // parseJarUrlFromAssets() — edge cases
    // -------------------------------------------------------------------------

    @Test
    void parseJarUrlFromAssets_emptyStringReturnsNull() {
        assertNull(service.parseJarUrlFromAssets(""));
    }

    @Test
    void parseJarUrlFromAssets_truncatedJsonReturnsNull() {
        // No closing bracket — bracket-depth tracking should handle gracefully
        String json = "{\"assets\":[{\"name\":\"Plugin-1.0.jar\",\"browser_download_url\":\"https://example.com/Plugin-1.0.jar\"";
        assertNull(service.parseJarUrlFromAssets(json));
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Logger capturingLogger(List<String> warnings) {
        return new Logger(null) {
            @Override public void log(String m) {}
            @Override public void warn(String m) { warnings.add(m); }
        };
    }

    @SuppressWarnings("resource")
    private HttpURLConnection fakeConnection(int statusCode, String rateLimitRemaining) throws IOException {
        return new HttpURLConnection(new URL("http://fake.invalid")) {
            @Override public void connect() {}
            @Override public void disconnect() {}
            @Override public boolean usingProxy() { return false; }
            @Override public int getResponseCode() { return statusCode; }
            @Override
            public String getHeaderField(String name) {
                if ("X-RateLimit-Remaining".equals(name)) return rateLimitRemaining;
                return null;
            }
            @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
            @Override public InputStream getInputStream() throws IOException {
                throw new IOException("not used in error-path tests");
            }
        };
    }
}
