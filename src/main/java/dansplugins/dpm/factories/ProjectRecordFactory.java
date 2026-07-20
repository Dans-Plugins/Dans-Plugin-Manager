package dansplugins.dpm.factories;

import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.objects.ProjectRecord;

public class ProjectRecordFactory {
    private final ProjectRecordRepository projectRecordRepository;

    public ProjectRecordFactory(ProjectRecordRepository projectRecordRepository) {
        this.projectRecordRepository = projectRecordRepository;
    }

    public void createGitHubRecord(String name, String owner, String repo) {
        projectRecordRepository.addProjectRecord(ProjectRecord.forGitHub(name, owner, repo));
    }

    public void register(ProjectRecord record) {
        projectRecordRepository.addProjectRecord(record);
    }
}
