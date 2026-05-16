package dansplugins.dpm.objects;

public class ProjectRecord {
    private final String name;
    private final String owner;
    private final String repo;
    private final String directLink;

    public ProjectRecord(String name, String owner, String repo) {
        this.name = name;
        this.owner = owner;
        this.repo = repo;
        this.directLink = null;
    }

    public ProjectRecord(String name, String directLink) {
        this.name = name;
        this.owner = null;
        this.repo = null;
        this.directLink = directLink;
    }

    public String getName() {
        return name;
    }

    public boolean isGitHubHosted() {
        return owner != null && repo != null;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

    public String getDirectLink() {
        return directLink;
    }
}
