package dansplugins.dpm.objects;

public class ReleaseInfo {
    /** Sentinel returned when a repo has no published release yet. */
    public static final ReleaseInfo NO_RELEASE = new ReleaseInfo(null, null);

    private final String tagName;
    private final String jarUrl;

    public ReleaseInfo(String tagName, String jarUrl) {
        this.tagName = tagName;
        this.jarUrl = jarUrl;
    }

    public String getTagName() {
        return tagName;
    }

    public String getJarUrl() {
        return jarUrl;
    }
}
