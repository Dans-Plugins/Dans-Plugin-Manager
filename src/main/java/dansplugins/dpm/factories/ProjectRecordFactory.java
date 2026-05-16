package dansplugins.dpm.factories;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;

public class ProjectRecordFactory {
    private final EphemeralData ephemeralData;

    public ProjectRecordFactory(EphemeralData ephemeralData) {
        this.ephemeralData = ephemeralData;
    }

    public void createGitHubRecord(String name, String owner, String repo) {
        ephemeralData.addProjectRecord(new ProjectRecord(name, owner, repo));
    }

    public void createDirectLinkRecord(String name, String directLink) {
        ephemeralData.addProjectRecord(new ProjectRecord(name, directLink));
    }
}
