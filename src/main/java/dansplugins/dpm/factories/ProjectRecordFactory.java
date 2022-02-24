package dansplugins.dpm.factories;

import dansplugins.dpm.data.EphemeralData;
import dansplugins.dpm.objects.ProjectRecord;

public class ProjectRecordFactory {
    private static ProjectRecordFactory instance;

    private ProjectRecordFactory() {

    }

    public static ProjectRecordFactory getInstance() {
        if (instance == null) {
            instance = new ProjectRecordFactory();
        }
        return instance;
    }

    public void createProjectRecord(String name, String link) {
        ProjectRecord projectRecord = new ProjectRecord(name, link);
        EphemeralData.getInstance().addProjectRecord(projectRecord);
    }
}