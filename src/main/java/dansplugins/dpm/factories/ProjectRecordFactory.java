package dansplugins.dpm.factories;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;

public class ProjectRecordFactory {
    private final EphemeralData ephemeralData;

    public ProjectRecordFactory(EphemeralData ephemeralData) {
        this.ephemeralData = ephemeralData;
    }

    public void createProjectRecord(String name, String link) {
        ProjectRecord projectRecord = new ProjectRecord(name, link);
        ephemeralData.addProjectRecord(projectRecord);
    }
}