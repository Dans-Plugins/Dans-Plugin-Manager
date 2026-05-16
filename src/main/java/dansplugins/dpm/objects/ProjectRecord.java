package dansplugins.dpm.objects;

import java.util.ArrayList;
import java.util.List;

public class ProjectRecord {
    private final String name;
    private final String owner;
    private final String repo;
    private final String description;
    private final List<String> hardDependencies;
    private final List<String> softDependencies;

    private ProjectRecord(Builder builder) {
        this.name = builder.name;
        this.owner = builder.owner;
        this.repo = builder.repo;
        this.description = builder.description;
        this.hardDependencies = List.copyOf(builder.hardDependencies);
        this.softDependencies = List.copyOf(builder.softDependencies);
    }

    public static ProjectRecord forGitHub(String name, String owner, String repo) {
        return new Builder(name, owner, repo).build();
    }

    public static Builder builder(String name, String owner, String repo) {
        return new Builder(name, owner, repo);
    }

    public String getName() { return name; }
    public String getOwner() { return owner; }
    public String getRepo() { return repo; }
    public String getDescription() { return description; }
    public List<String> getHardDependencies() { return hardDependencies; }
    public List<String> getSoftDependencies() { return softDependencies; }

    public static final class Builder {
        private final String name;
        private final String owner;
        private final String repo;
        private String description;
        private List<String> hardDependencies = new ArrayList<>();
        private List<String> softDependencies = new ArrayList<>();

        private Builder(String name, String owner, String repo) {
            this.name = name;
            this.owner = owner;
            this.repo = repo;
        }

        public Builder description(String d) {
            this.description = d;
            return this;
        }

        public Builder hardDependencies(List<String> deps) {
            this.hardDependencies = new ArrayList<>(deps);
            return this;
        }

        public Builder softDependencies(List<String> deps) {
            this.softDependencies = new ArrayList<>(deps);
            return this;
        }

        public ProjectRecord build() {
            return new ProjectRecord(this);
        }
    }
}
