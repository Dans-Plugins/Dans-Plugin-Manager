package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Performs the /dpm list installed/version lookups, returning plain result data rather than
// sending messages. ListCommand formats and sends the results.
public class ListController {

    public static final class ListEntry {
        private final ProjectRecord record;
        private final boolean installed;
        private final String storedTag;

        ListEntry(ProjectRecord record, boolean installed, String storedTag) {
            this.record = record;
            this.installed = installed;
            this.storedTag = storedTag;
        }

        public ProjectRecord getRecord() { return record; }
        public boolean isInstalled() { return installed; }
        public String getStoredTag() { return storedTag; }
    }

    private final ProjectRecordRepository projectRecordRepository;
    private final PluginFileRepository pluginFileRepository;
    private final VersionRepository versionRepository;

    public ListController(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository,
                          VersionRepository versionRepository) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.versionRepository = versionRepository;
    }

    public List<ListEntry> listAll() {
        List<ProjectRecord> records = projectRecordRepository.getAllProjectRecords();
        Set<String> installedNames = installedNames(records);
        List<ListEntry> entries = new ArrayList<>();
        for (ProjectRecord record : records) {
            entries.add(toEntry(record, installedNames.contains(record.getName())));
        }
        return entries;
    }

    public List<ListEntry> listInstalled() {
        List<ProjectRecord> installed = pluginFileRepository.filterInstalled(projectRecordRepository.getAllProjectRecords());
        List<ListEntry> entries = new ArrayList<>();
        for (ProjectRecord record : installed) {
            entries.add(toEntry(record, true));
        }
        return entries;
    }

    // Available plugins are by definition not installed and carry no stored tag, so the records
    // themselves are returned rather than entries with two dead fields.
    public List<ProjectRecord> listAvailable() {
        List<ProjectRecord> records = projectRecordRepository.getAllProjectRecords();
        Set<String> installedNames = installedNames(records);
        List<ProjectRecord> available = new ArrayList<>();
        for (ProjectRecord record : records) {
            if (!installedNames.contains(record.getName())) available.add(record);
        }
        return available;
    }

    private ListEntry toEntry(ProjectRecord record, boolean installed) {
        String storedTag = installed ? versionRepository.getStoredTag(record.getName()) : null;
        return new ListEntry(record, installed, storedTag);
    }

    private Set<String> installedNames(List<ProjectRecord> records) {
        Set<String> names = new HashSet<>();
        for (ProjectRecord record : pluginFileRepository.filterInstalled(records)) {
            names.add(record.getName());
        }
        return names;
    }
}
