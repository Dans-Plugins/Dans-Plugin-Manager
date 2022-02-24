package dansplugins.dpm.data;

import java.util.ArrayList;

import dansplugins.dpm.objects.ProjectRecord;

public class EphemeralData {
    private static EphemeralData instance;
    private final ArrayList<ProjectRecord> projectRecords = new ArrayList<>();

    private EphemeralData() {

    }

    public static EphemeralData getInstance() {
        if (instance == null) {
            instance = new EphemeralData();
        }
        return instance;
    }

    public ProjectRecord getProjectRecord(String name) {
        for (ProjectRecord projectRecord : projectRecords) {
            if (projectRecord.getName().equalsIgnoreCase(name)) {
                return projectRecord;
            }
        }
        return null;
    }

    public boolean addProjectRecord(ProjectRecord projectRecord) {
        return projectRecords.add(projectRecord);
    }

    public boolean removeProjectRecord(ProjectRecord projectRecord) {
        return projectRecords.remove(projectRecord);
    }
}
