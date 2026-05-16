package dansplugins.dpm.services;

import org.junit.jupiter.api.Test;

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
    // setApiToken()
    // -------------------------------------------------------------------------

    @Test
    void setApiToken_nullTreatedAsEmpty() {
        // Should not throw; null is coerced to empty string internally.
        assertDoesNotThrow(() -> service.setApiToken(null));
    }

    @Test
    void setApiToken_emptyStringAccepted() {
        assertDoesNotThrow(() -> service.setApiToken(""));
    }

    @Test
    void setApiToken_validTokenAccepted() {
        assertDoesNotThrow(() -> service.setApiToken("ghp_exampletoken123"));
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
}
