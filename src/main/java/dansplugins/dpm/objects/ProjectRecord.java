package dansplugins.dpm.objects;

public class ProjectRecord {
    private final String name;
    private final String owner;
    private final String repo;

    private ProjectRecord(String name, String owner, String repo) {
        this.name = name;
        this.owner = owner;
        this.repo = repo;
    }

    public static ProjectRecord forGitHub(String name, String owner, String repo) {
        return new ProjectRecord(name, owner, repo);
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }
}
