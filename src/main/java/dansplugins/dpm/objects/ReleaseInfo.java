package dansplugins.dpm.objects;

public class ReleaseInfo {
    /** Sentinel returned when a repo has no published release yet. */
    public static final ReleaseInfo NO_RELEASE = new ReleaseInfo(null, null, null);

    private final String tagName;
    private final String jarUrl;
    private final String publishedAt;

    public ReleaseInfo(String tagName, String jarUrl) {
        this(tagName, jarUrl, null);
    }

    public ReleaseInfo(String tagName, String jarUrl, String publishedAt) {
        this.tagName = tagName;
        this.jarUrl = jarUrl;
        this.publishedAt = publishedAt;
    }

    public String getTagName() {
        return tagName;
    }

    public String getJarUrl() {
        return jarUrl;
    }

    public String getPublishedAt() {
        return publishedAt;
    }
}
