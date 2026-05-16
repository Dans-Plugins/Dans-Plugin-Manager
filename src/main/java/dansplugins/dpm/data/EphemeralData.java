package dansplugins.dpm.data;

import dansplugins.dpm.objects.ProjectRecord;

import java.util.ArrayList;
import java.util.List;

public class EphemeralData {
    private final ArrayList<ProjectRecord> projectRecords = new ArrayList<>();

    public ProjectRecord getProjectRecord(String name) {
        for (ProjectRecord projectRecord : projectRecords) {
            if (projectRecord.getName().equalsIgnoreCase(name)) {
                return projectRecord;
            }
        }
        return null;
    }

    public void addProjectRecord(ProjectRecord projectRecord) {
        projectRecords.add(projectRecord);
    }

    public boolean removeProjectRecord(ProjectRecord projectRecord) {
        return projectRecords.remove(projectRecord);
    }

    public List<ProjectRecord> getAllProjectRecords() {
        return new ArrayList<>(projectRecords);
    }

    public int getNumProjectRecords() {
        return projectRecords.size();
    }
}
