package dansplugins.dpm.controllers;

import dansplugins.dpm.objects.ProjectRecord;
import dansplugins.dpm.objects.ReleaseChannel;
import dansplugins.dpm.objects.ReleaseInfo;
import dansplugins.dpm.repositories.ChannelRepository;
import dansplugins.dpm.repositories.GitHubReleaseRepository;
import dansplugins.dpm.repositories.PluginFileRepository;
import dansplugins.dpm.repositories.ProjectRecordRepository;
import dansplugins.dpm.repositories.VersionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Performs the /dpm info record lookup, release-metadata fetch, and install-status resolution for
// a plugin and its declared dependencies, returning plain result data rather than sending messages.
// InfoCommand formats and sends the results.
public class InfoController {

    public static final class PluginInfo {
        private final ProjectRecord record;
        private final ReleaseInfo release;
        private final ReleaseChannel channel;
        private final boolean installed;
        private final String storedTag;
        private final Set<String> installedNames;

        PluginInfo(ProjectRecord record, ReleaseInfo release, ReleaseChannel channel, boolean installed, String storedTag, Set<String> installedNames) {
            this.record = record;
            this.release = release;
            this.channel = channel;
            this.installed = installed;
            this.storedTag = storedTag;
            this.installedNames = installedNames;
        }

        public ProjectRecord getRecord() { return record; }
        public ReleaseInfo getRelease() { return release; }
        public ReleaseChannel getChannel() { return channel; }
        public boolean isInstalled() { return installed; }
        public String getStoredTag() { return storedTag; }

        /** True when a release was fetched successfully and the repo has one published. */
        public boolean hasPublishedRelease() {
            return release != null && release != ReleaseInfo.NO_RELEASE;
        }

        /** True when the plugin is installed and the installed tag matches the latest published release tag. */
        public boolean isUpToDate() {
            return installed && hasPublishedRelease() && storedTag != null && storedTag.equals(release.getTagName());
        }

        public boolean isDependencyInstalled(String pluginName) {
            return installedNames.contains(pluginName);
        }
    }

    private final ProjectRecordRepository projectRecordRepository;
    private final GitHubReleaseRepository gitHubReleaseRepository;
    private final PluginFileRepository pluginFileRepository;
    private final VersionRepository versionRepository;
    private final ChannelRepository channelRepository;

    public InfoController(ProjectRecordRepository projectRecordRepository, GitHubReleaseRepository gitHubReleaseRepository,
                          PluginFileRepository pluginFileRepository, VersionRepository versionRepository,
                          ChannelRepository channelRepository) {
        this.projectRecordRepository = projectRecordRepository;
        this.gitHubReleaseRepository = gitHubReleaseRepository;
        this.pluginFileRepository = pluginFileRepository;
        this.versionRepository = versionRepository;
        this.channelRepository = channelRepository;
    }

    public ProjectRecord getRecord(String name) {
        return projectRecordRepository.getProjectRecord(name);
    }

    // Touches the GitHub API — callers must invoke this off the main thread.
    public PluginInfo getInfo(ProjectRecord record) {
        ReleaseChannel channel = channelRepository.getChannel(record.getName());
        ReleaseInfo release = gitHubReleaseRepository.getReleaseMetadata(record.getOwner(), record.getRepo(), channel);
        Set<String> installedNames = installedNamesFor(record);
        boolean installed = installedNames.contains(record.getName());
        String storedTag = versionRepository.getStoredTag(record.getName());
        return new PluginInfo(record, release, channel, installed, storedTag, installedNames);
    }

    private Set<String> installedNamesFor(ProjectRecord record) {
        List<ProjectRecord> toScan = new ArrayList<>();
        toScan.add(record);
        addDependencyRecords(toScan, record.getHardDependencies());
        addDependencyRecords(toScan, record.getSoftDependencies());
        Set<String> names = new HashSet<>();
        for (ProjectRecord r : pluginFileRepository.filterInstalled(toScan)) {
            names.add(r.getName());
        }
        return names;
    }

    private void addDependencyRecords(List<ProjectRecord> toScan, List<String> dependencyNames) {
        for (String dependencyName : dependencyNames) {
            ProjectRecord record = projectRecordRepository.getProjectRecord(dependencyName);
            if (record != null) toScan.add(record);
        }
    }
}

