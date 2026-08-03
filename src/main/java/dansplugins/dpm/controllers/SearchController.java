package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Performs the /dpm search keyword filtering and installed/version lookup, returning plain
// result data rather than sending messages. SearchCommand formats and sends the results.
public class SearchController {

    public static final class SearchResult {
        private final ProjectRecord record;
        private final boolean installed;
        private final String storedTag;

        SearchResult(ProjectRecord record, boolean installed, String storedTag) {
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

    public SearchController(ProjectRecordRepository projectRecordRepository, PluginFileRepository pluginFileRepository,
                            VersionRepository versionRepository) {
        this.projectRecordRepository = projectRecordRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.versionRepository = versionRepository;
    }

    public List<SearchResult> search(String keyword) {
        List<ProjectRecord> matches = new ArrayList<>();
        for (ProjectRecord record : projectRecordRepository.getAllProjectRecords()) {
            if (matchesKeyword(record, keyword)) matches.add(record);
        }
        if (matches.isEmpty()) {
            return List.of();
        }
        Set<String> installedNames = new HashSet<>();
        for (ProjectRecord r : pluginFileRepository.filterInstalled(matches)) {
            installedNames.add(r.getName());
        }
        List<SearchResult> results = new ArrayList<>();
        for (ProjectRecord record : matches) {
            boolean installed = installedNames.contains(record.getName());
            String storedTag = installed ? versionRepository.getStoredTag(record.getName()) : null;
            results.add(new SearchResult(record, installed, storedTag));
        }
        return results;
    }

    static boolean matchesKeyword(ProjectRecord record, String keyword) {
        String kw = keyword.toLowerCase();
        if (record.getName().toLowerCase().contains(kw)) return true;
        return record.getDescription() != null
                && record.getDescription().toLowerCase().contains(kw);
    }
}
