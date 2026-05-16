package dansplugins.dpm.objects;

public class ProjectRecord {
    private final String name;
    private final String owner;
    private final String repo;
    private final String directLink;

    private ProjectRecord(String name, String owner, String repo, String directLink) {
        this.name = name;
        this.owner = owner;
        this.repo = repo;
        this.directLink = directLink;
    }

    public static ProjectRecord forGitHub(String name, String owner, String repo) {
        return new ProjectRecord(name, owner, repo, null);
    }

    public static ProjectRecord forDirectLink(String name, String directLink) {
        return new ProjectRecord(name, null, null, directLink);
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
