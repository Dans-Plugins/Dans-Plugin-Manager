package dansplugins.dpm.repositories;

import dansplugins.dpm.objects.ReleaseChannel;
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

class GitHubReleaseRepositoryTest {

    // Logger is only used for HTTP errors; null is safe for pure parsing tests.
    private final GitHubReleaseRepository service = new GitHubReleaseRepository(null);

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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
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
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
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
    // doFetch() — retry on transient failures (#85)
    // -------------------------------------------------------------------------

    @Test
    void doFetch_retriesOnce_afterIoException() throws IOException {
        int[] callCount = {0};
        String successJson = "{\"tag_name\":\"v1.0\",\"published_at\":\"2024-01-01T00:00:00Z\"," +
                "\"assets\":[{\"browser_download_url\":\"https://example.com/plugin.jar\"}]}";
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override void sleepMs(long ms) {}
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                if (callCount[0]++ == 0) throw new IOException("transient network error");
                return fakeConnection(200, null, successJson);
            }
        };
        ReleaseInfo result = svc.doFetch("org", "repo");
        assertNotNull(result, "Second attempt must succeed after transient IOException");
        assertEquals("v1.0", result.getTagName());
        assertEquals(2, callCount[0], "openConnection must be called exactly twice");
    }

    @Test
    void doFetch_retriesOnce_after5xxResponse() throws IOException {
        int[] callCount = {0};
        String successJson = "{\"tag_name\":\"v2.0\",\"published_at\":\"2024-01-01T00:00:00Z\"," +
                "\"assets\":[{\"browser_download_url\":\"https://example.com/plugin.jar\"}]}";
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override void sleepMs(long ms) {}
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                return callCount[0]++ == 0 ? fakeConnection(503, null, null) : fakeConnection(200, null, successJson);
            }
        };
        ReleaseInfo result = svc.doFetch("org", "repo");
        assertNotNull(result, "Second attempt must succeed after transient 5xx response");
        assertEquals("v2.0", result.getTagName());
        assertEquals(2, callCount[0], "openConnection must be called exactly twice");
    }

    @Test
    void doFetch_doesNotRetryOn4xxClientError() throws IOException {
        int[] callCount = {0};
        List<String> warnings = new ArrayList<>();
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
            @Override void sleepMs(long ms) {}
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                callCount[0]++;
                return fakeConnection(401, null);
            }
        };
        assertNull(svc.doFetch("org", "repo"));
        assertEquals(1, callCount[0], "4xx must not be retried — only one connection attempt");
    }

    @Test
    void doFetch_returnsNull_afterBothAttemptsFailWithIoException() throws IOException {
        int[] callCount = {0};
        List<String> warnings = new ArrayList<>();
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
            @Override void sleepMs(long ms) {}
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                callCount[0]++;
                throw new IOException("persistent failure");
            }
        };
        assertNull(svc.doFetch("org", "repo"));
        assertEquals(2, callCount[0], "Both attempts must be made before giving up");
        assertFalse(warnings.isEmpty(), "Warning must be emitted after both attempts fail");
    }

    @Test
    void doFetch_returnsNull_afterBoth5xxAttemptsFail() throws IOException {
        int[] callCount = {0};
        List<String> warnings = new ArrayList<>();
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
            @Override void sleepMs(long ms) {}
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                callCount[0]++;
                return fakeConnection(503, null);
            }
        };
        assertNull(svc.doFetch("org", "repo"));
        assertEquals(2, callCount[0], "Both attempts must be made before giving up on persistent 5xx");
        assertFalse(warnings.isEmpty(), "Warning must be emitted after both 5xx attempts fail");
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
    // synthesizeExperimentalVersion()
    // -------------------------------------------------------------------------

    @Test
    void synthesizeExperimentalVersion_usesShortShaWhenTargetCommitishIsACommitSha() {
        assertEquals("dev-abc1234",
                service.synthesizeExperimentalVersion("dev", "abc1234def5678901234567890123456789abcde", "2024-01-01T00:00:00Z"));
    }

    @Test
    void synthesizeExperimentalVersion_fallsBackToPublishedAtWhenTargetCommitishIsABranchName() {
        assertEquals("dev@2024-03-15T10:00:00Z",
                service.synthesizeExperimentalVersion("dev", "main", "2024-03-15T10:00:00Z"));
    }

    @Test
    void synthesizeExperimentalVersion_fallsBackToTagWhenNeitherShaNorPublishedAtAvailable() {
        assertEquals("dev", service.synthesizeExperimentalVersion("dev", "main", null));
        assertEquals("dev", service.synthesizeExperimentalVersion("dev", null, ""));
    }

    @Test
    void synthesizeExperimentalVersion_usesConfiguredTagWhenReleaseTagNameIsMissing() {
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null);
        svc.setExperimentalTag("nightly");
        assertEquals("nightly-abc1234",
                svc.synthesizeExperimentalVersion(null, "abc1234def5678901234567890123456789abcde", null));
    }

    @Test
    void synthesizeExperimentalVersion_rejectsNonHexAndWrongLengthCommitish() {
        // 40 characters but not hex — must not be mistaken for a sha.
        assertEquals("dev@2024-01-01T00:00:00Z",
                service.synthesizeExperimentalVersion("dev", "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz", "2024-01-01T00:00:00Z"));
        // A short sha is not a full commitish and must not be truncated silently.
        assertEquals("dev@2024-01-01T00:00:00Z",
                service.synthesizeExperimentalVersion("dev", "abc1234", "2024-01-01T00:00:00Z"));
    }

    // -------------------------------------------------------------------------
    // parseTargetCommitish()
    // -------------------------------------------------------------------------

    @Test
    void parseTargetCommitish_returnsValue() {
        String json = "{\"tag_name\":\"dev\",\"target_commitish\":\"abc1234def5678901234567890123456789abcde\"}";
        assertEquals("abc1234def5678901234567890123456789abcde", service.parseTargetCommitish(json));
    }

    @Test
    void parseTargetCommitish_returnsNullWhenMissing() {
        assertNull(service.parseTargetCommitish("{\"tag_name\":\"dev\"}"));
    }

    // -------------------------------------------------------------------------
    // getExperimentalRelease()
    // -------------------------------------------------------------------------

    @Test
    void getExperimentalRelease_requestsTheConfiguredTagAndSynthesizesAVersionFromTheCommit() throws IOException {
        List<String> requestedUrls = new ArrayList<>();
        String json = "{\"tag_name\":\"dev\",\"target_commitish\":\"abc1234def5678901234567890123456789abcde\"," +
                "\"published_at\":\"2024-05-01T00:00:00Z\"," +
                "\"assets\":[{\"browser_download_url\":\"https://example.com/plugin.jar\"}]}";
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                requestedUrls.add(url);
                return fakeConnection(200, null, json);
            }
        };

        ReleaseInfo release = svc.getExperimentalRelease("org", "repo");

        assertNotNull(release);
        assertEquals("dev-abc1234", release.getTagName());
        assertEquals("https://example.com/plugin.jar", release.getJarUrl());
        assertEquals(List.of("https://api.github.com/repos/org/repo/releases/tags/dev"), requestedUrls);
    }

    @Test
    void getExperimentalRelease_honoursACustomExperimentalTag() throws IOException {
        List<String> requestedUrls = new ArrayList<>();
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                requestedUrls.add(url);
                return fakeConnection(404, null);
            }
        };
        svc.setExperimentalTag("nightly");

        svc.getExperimentalRelease("org", "repo");

        assertEquals(List.of("https://api.github.com/repos/org/repo/releases/tags/nightly"), requestedUrls);
    }

    @Test
    void getExperimentalRelease_returnsNoReleaseWhenTagDoesNotExist() throws IOException {
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                return fakeConnection(404, null);
            }
        };

        assertSame(ReleaseInfo.NO_RELEASE, svc.getExperimentalRelease("org", "repo"),
                "A repository that publishes no experimental build must report NO_RELEASE, not an error");
    }

    @Test
    void getExperimentalRelease_returnsNullAndWarnsWhenPrereleaseHasNoJarAsset() throws IOException {
        List<String> warnings = new ArrayList<>();
        String json = "{\"tag_name\":\"dev\",\"target_commitish\":\"abc1234def5678901234567890123456789abcde\"," +
                "\"assets\":[{\"browser_download_url\":\"https://example.com/checksums.txt\"}]}";
        GitHubReleaseRepository svc = new GitHubReleaseRepository(capturingLogger(warnings)) {
            @Override
            HttpURLConnection openConnection(String url) throws IOException {
                return fakeConnection(200, null, json);
            }
        };

        assertNull(svc.getExperimentalRelease("org", "repo"));
        assertTrue(warnings.stream().anyMatch(m -> m.contains("no .jar asset")),
                "A dev prerelease with no JAR attached must warn");
    }

    @Test
    void setExperimentalTag_blankOrNullRestoresTheDefault() {
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null);
        svc.setExperimentalTag("nightly");
        svc.setExperimentalTag("   ");
        assertEquals("dev", svc.getExperimentalTag());
        svc.setExperimentalTag("nightly");
        svc.setExperimentalTag(null);
        assertEquals("dev", svc.getExperimentalTag());
    }

    // -------------------------------------------------------------------------
    // channel-aware caching
    // -------------------------------------------------------------------------

    @Test
    void cache_keepsStableAndExperimentalEntriesSeparateForTheSameRepo() {
        AtomicInteger stableFetches = new AtomicInteger(0);
        AtomicInteger experimentalFetches = new AtomicInteger(0);
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                stableFetches.incrementAndGet();
                return new ReleaseInfo("v1.0", "https://example.com/stable.jar", null);
            }
            @Override
            ReleaseInfo doFetchExperimental(String owner, String repo) {
                experimentalFetches.incrementAndGet();
                return new ReleaseInfo("dev-abc1234", "https://example.com/dev.jar", null);
            }
        };

        assertEquals("v1.0", svc.getLatestRelease("org", "repo").getTagName());
        assertEquals("dev-abc1234", svc.getExperimentalRelease("org", "repo").getTagName(),
                "The experimental fetch must not be served the cached stable release");
        assertEquals("v1.0", svc.getLatestRelease("org", "repo").getTagName(),
                "The stable fetch must not be served the cached experimental release");

        assertEquals(1, stableFetches.get(), "Stable should be fetched once and then cached");
        assertEquals(1, experimentalFetches.get(), "Experimental should be fetched once and then cached");
    }

    @Test
    void cacheKey_changesWithTheConfiguredExperimentalTag() {
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null);
        String stableKey = svc.cacheKey("org", "repo", ReleaseChannel.STABLE);
        String devKey = svc.cacheKey("org", "repo", ReleaseChannel.EXPERIMENTAL);
        svc.setExperimentalTag("nightly");
        String nightlyKey = svc.cacheKey("org", "repo", ReleaseChannel.EXPERIMENTAL);

        assertNotEquals(stableKey, devKey);
        assertNotEquals(devKey, nightlyKey);
    }

    @Test
    void clearCache_refetchesBothChannels() {
        AtomicInteger stableFetches = new AtomicInteger(0);
        AtomicInteger experimentalFetches = new AtomicInteger(0);
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                stableFetches.incrementAndGet();
                return new ReleaseInfo("v1.0", "https://example.com/stable.jar", null);
            }
            @Override
            ReleaseInfo doFetchExperimental(String owner, String repo) {
                experimentalFetches.incrementAndGet();
                return new ReleaseInfo("dev-abc1234", "https://example.com/dev.jar", null);
            }
        };
        svc.getLatestRelease("org", "repo");
        svc.getExperimentalRelease("org", "repo");
        svc.clearCache();
        svc.getLatestRelease("org", "repo");
        svc.getExperimentalRelease("org", "repo");

        assertEquals(2, stableFetches.get());
        assertEquals(2, experimentalFetches.get());
    }

    @Test
    void getRelease_dispatchesOnChannel() {
        GitHubReleaseRepository svc = new GitHubReleaseRepository(null) {
            @Override
            ReleaseInfo doFetch(String owner, String repo) {
                return new ReleaseInfo("v1.0", "https://example.com/stable.jar", null);
            }
            @Override
            ReleaseInfo doFetchExperimental(String owner, String repo) {
                return new ReleaseInfo("dev-abc1234", "https://example.com/dev.jar", null);
            }
        };

        assertEquals("v1.0", svc.getRelease("org", "repo", ReleaseChannel.STABLE).getTagName());
        assertEquals("dev-abc1234", svc.getRelease("org", "repo", ReleaseChannel.EXPERIMENTAL).getTagName());
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
        return fakeConnection(statusCode, rateLimitRemaining, null);
    }

    @SuppressWarnings("resource")
    private HttpURLConnection fakeConnection(int statusCode, String rateLimitRemaining, String responseBody) throws IOException {
        byte[] bodyBytes = responseBody != null ? responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0];
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
                if (statusCode != 200) throw new IOException("not used in error-path tests");
                return new ByteArrayInputStream(bodyBytes);
            }
        };
    }
}
